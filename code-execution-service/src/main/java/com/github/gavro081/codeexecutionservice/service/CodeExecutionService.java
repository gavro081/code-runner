package com.github.gavro081.codeexecutionservice.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.LogConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.gavro081.codeexecutionservice.models.ExecutionResult;
import com.github.gavro081.common.model.ProgrammingLanguage;
import jakarta.annotation.PostConstruct;
import com.github.dockerjava.api.model.HostConfig;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;


@Service
public class CodeExecutionService {
    private DockerClient dockerClient;
    private final Map<ProgrammingLanguage, LanguageConfig> languageConfigMap = new EnumMap<>(ProgrammingLanguage.class);

    // limit how many bytes we keep in-memory for stdout/stderr
    private static final int MAX_CAPTURE_BYTES = 64 * 1024; // 64 KB

    // docker json-file log driver rotation options (limits on-disk logs)
    private static final String LOG_MAX_SIZE = "64k";
    private static final String LOG_MAX_FILES = "1";

    private record LanguageConfig(String imageName, String... command){}

    @PostConstruct
    public void init(){
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(30)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);

        languageConfigMap.put(
                ProgrammingLanguage.PYTHON,
                new LanguageConfig("python-coderunner-image", "python", "-c"));

        languageConfigMap.put(
                ProgrammingLanguage.JAVASCRIPT,
                new LanguageConfig("javascript-coderunner-image", "node", "-e"));
    }

    public ExecutionResult execute(String code, ProgrammingLanguage language){
        LanguageConfig config = languageConfigMap.get(language);
        if (config == null)
            return ExecutionResult.builder()
                    .stderr("Language not supported")
                    .exitCode(1)
                    .build();

        // configure container log rotation to avoid unbounded on-disk growth
        Map<String, String> logOptions = Map.of(
                "max-size", LOG_MAX_SIZE,
                "max-file", LOG_MAX_FILES
        );

        HostConfig hostConfig = new HostConfig()
                .withMemory(100L * 1024 * 1024) // 100MB
                .withMemorySwap(100L * 1024 * 1024) // disable swap by setting swap == memory
                .withNanoCPUs(500_000_000L) // 0.5 CPU cores
                .withNetworkMode("none")
                .withPidsLimit(50L)
                .withReadonlyRootfs(true)
                .withAutoRemove(true)
                .withLogConfig((new LogConfig(LogConfig.LoggingType.JSON_FILE, logOptions)));

        String []command = {config.command[0], config.command[1], code};

        CreateContainerResponse container = dockerClient.createContainerCmd(config.imageName)
                .withHostConfig(hostConfig)
                .withCmd(command)
                .withUser("coder")
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        String containerId = container.getId();

        dockerClient.startContainerCmd(containerId).exec();
        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();
        final AtomicBoolean stdoutTruncated = new AtomicBoolean(false);
        final AtomicBoolean stderrTruncated = new AtomicBoolean(false);
        int exitCode = -1;

        try (ResultCallback.Adapter<Frame> logCallback = dockerClient
                .logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .exec(new ResultCallback.Adapter<>() {
                    @Override
                    public void onNext(Frame object) {
                        byte[] payload = object.getPayload() == null ? new byte[0] : object.getPayload();
                        if (object.getStreamType() == StreamType.STDOUT) {
                            if (!stdoutTruncated.get()) {
                                int remaining = MAX_CAPTURE_BYTES - stdout.length();
                                if (remaining <= 0) {
                                    stdoutTruncated.set(true);
                                    return;
                                }
                                int toAppend = Math.min(remaining, payload.length);
                                stdout.append(new String(payload, 0, toAppend, StandardCharsets.UTF_8));
                                if (toAppend < payload.length || stdout.length() >= MAX_CAPTURE_BYTES) {
                                    stdoutTruncated.set(true);
                                }
                            }
                        } else if (object.getStreamType() == StreamType.STDERR) {
                            if (!stderrTruncated.get()) {
                                int remaining = MAX_CAPTURE_BYTES - stderr.length();
                                if (remaining <= 0) {
                                    stderrTruncated.set(true);
                                    return;
                                }
                                int toAppend = Math.min(remaining, payload.length);
                                stderr.append(new String(payload, 0, toAppend, StandardCharsets.UTF_8));
                                if (toAppend < payload.length || stderr.length() >= MAX_CAPTURE_BYTES) {
                                    stderrTruncated.set(true);
                                }
                            }
                        }
                    }
                }))
    {

            try {
                exitCode = dockerClient.waitContainerCmd(containerId)
                        .exec(new WaitContainerResultCallback())
                        .awaitStatusCode(10, TimeUnit.SECONDS);

            } catch (Exception e) {
                // timeout occurred
                // we must manually kill the container as waitContainerCmd doesn't
                dockerClient.killContainerCmd(containerId);
                stderr.append("Execution timed out.");
                return ExecutionResult.builder()
                        .stdout(stdout.toString())
                        .stderr(stderr.toString())
                        .exitCode(137)
                        .build();
            }

            logCallback.awaitCompletion(2, TimeUnit.SECONDS);

        } catch (Exception e) {
            stderr.append("Error attaching or reading logs: ").append(e.getMessage());
            if (exitCode == -1) {
                try {
                    // attempt to get the exit code if logs failed
                    Long codeLong = dockerClient.inspectContainerCmd(containerId).exec().getState().getExitCodeLong();
                    if (codeLong != null) exitCode = codeLong.intValue();
                } catch (Exception ignored) {}
            }
        }

        if (stdoutTruncated.get()) {
            stderr.append("\n[stdout truncated]");
        }
        if (stderrTruncated.get()) {
            stderr.append("\n[stderr truncated]");
        }

        return ExecutionResult.builder()
                .stdout(stdout.toString())
                .stderr(stderr.toString())
                .exitCode(exitCode)
                .build();

    }
}
