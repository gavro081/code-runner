package com.github.gavro081.codeexecutionservice.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.gavro081.codeexecutionservice.models.TestCasesProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.gavro081.codeexecutionservice.models.ExecutionResult;
import com.github.gavro081.common.events.JobCreatedEvent;
import com.github.gavro081.common.model.Problem;
import com.github.gavro081.common.model.ProgrammingLanguage;
import com.github.gavro081.common.model.TestCase;

class FullSystemIntegrationTest {

    private CodeExecutionService codeExecutionService;
    private ProblemService problemService;
    private ObjectMapper objectMapper;
    private Map<String, Problem> problemsMap;

    @BeforeEach
    void setUp() throws IOException {
        problemService = mock(ProblemService.class);
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ResourceLoader resourceLoader = new DefaultResourceLoader();

        codeExecutionService = new CodeExecutionService(problemService, objectMapper, resourceLoader);
        codeExecutionService.init(); // docker client init

        // todo: maybe fetch actual database for problems instead of reading from json file
        // load problems from JSON
        File problemsFile = new File("../code_execution_db.problems.json");
        if (!problemsFile.exists()) {
            // fallback if running from root
            problemsFile = new File("code_execution_db.problems.json");
        }

        if (!problemsFile.exists()) {
            throw new RuntimeException("Could not find code_execution_db.problems.json");
        }

        List<Problem> problems = objectMapper.readValue(problemsFile, new TypeReference<List<Problem>>() {});
        problemsMap = problems.stream().collect(Collectors.toMap(Problem::getId, Function.identity()));

        // mock ProblemService
        when(problemService.getTestCases(anyString())).thenAnswer(invocation -> {
            String problemId = invocation.getArgument(0);
            Problem problem = problemsMap.get(problemId);
            if (problem == null) {
                throw new RuntimeException("Problem not found: " + problemId);
            }
            return new TestCasesProjection() {
                @Override
                public List<TestCase> getTestCases() {
                    return problem.getTestCases();
                }

                @Override
                public String getMethodName() {
                    return problem.getMethodName();
                }
            };
        });
    }

    @Test
    void testAllSolutions() throws IOException {
        Path solutionsDir = Paths.get("src/test/resources/solutions");
        if (!Files.exists(solutionsDir)) {
            // try absolute path if running from root
            solutionsDir = Paths.get("code-execution-service/src/test/resources/solutions");
        }

        if (!Files.exists(solutionsDir)) {
            System.out.println("No solutions directory found, skipping test.");
            return;
        }

        try (Stream<Path> paths = Files.walk(solutionsDir)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                String filename = file.getFileName().toString();
                if (!filename.endsWith(".py") && !filename.endsWith(".js")) {
                    return;
                }

                String problemId = filename.substring(0, filename.lastIndexOf('.'));
                String extension = filename.substring(filename.lastIndexOf('.') + 1);
                ProgrammingLanguage language = getLanguageFromExtension(extension);

                System.out.println("Testing solution for problem: " + problemId + " in " + language);

                try {
                    String code = Files.readString(file);
                    JobCreatedEvent job = new JobCreatedEvent(
                            UUID.randomUUID(),
                            problemId,
                            Instant.now(),
                            language,
                            code
                    );

                    ExecutionResult result = codeExecutionService.execute(job);

                    if (result.getExitCode() != 0) {
                        System.err.println("Execution failed for " + filename);
                        System.err.println("Stdout: " + result.getStdout());
                        System.err.println("Stderr: " + result.getStderr());
                    }

                    assertEquals(0, result.getExitCode(), "Exit code should be 0 for " + filename);
                    assertTrue(result.getStdout().contains("PASSED ALL TEST CASES"), "Output should contain 'PASSED ALL TEST CASES' for " + filename);

                } catch (IOException e) {
                    fail("Failed to read solution file: " + filename);
                }
            });
        }
    }

    private ProgrammingLanguage getLanguageFromExtension(String extension) {
        return switch (extension) {
            case "py" -> ProgrammingLanguage.PYTHON;
            case "js" -> ProgrammingLanguage.JAVASCRIPT;
            default -> throw new IllegalArgumentException("Unknown extension: " + extension);
        };
    }
}
