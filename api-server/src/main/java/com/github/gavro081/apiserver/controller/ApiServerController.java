package com.github.gavro081.apiserver.controller;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.dto.JobStatusDto;
import com.github.gavro081.apiserver.dto.ProblemDto;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.service.JobService;
import com.github.gavro081.apiserver.service.ProblemService;
import com.github.gavro081.common.model.Problem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiServerController {
    private final JobService jobService;
    private final ProblemService problemService;

    @Value("${server.port}")
    private String port;

    public ApiServerController(JobService jobService, ProblemService problemService) {
        this.jobService = jobService;
        this.problemService = problemService;
    }

    @GetMapping
    String index(){
        return "api index: " + port;
    }

    @PostMapping("/submit")
    ResponseEntity<Map<String, UUID>> submitCode(@Valid @RequestBody CodeSubmissionDto codeSubmissionDto){
        UUID jobId = jobService.createJob(codeSubmissionDto);
        return ResponseEntity.accepted().body(Map.of("job_id", jobId));
    }

    @GetMapping("/problems/{problemId}")
    ResponseEntity<ProblemDto> getProblem(@PathVariable @NotNull String problemId){
        try {
            Problem p = problemService.getProblem(problemId);
            ProblemDto problemDto = ProblemDto.builder()
                    .title(p.getTitle())
                    .assumptions(p.getAssumptions())
                    .difficulty(p.getDifficulty())
                    .exampleTestCases(p.getExampleTestCases())
                    .description(p.getDescription())
                    .starterTemplates(p.getStarterTemplates())
                    .constraints(p.getConstraints())
                    .build();
            return ResponseEntity.ok(problemDto);
        } catch (Exception e){
            return ResponseEntity.notFound().build();
        }

    }

    @GetMapping("/problems")
    ResponseEntity<List<Problem>> getProblems(){
        return ResponseEntity.ok(problemService.getProblems());
    }


    @GetMapping("/status/{jobId}")
    ResponseEntity<JobStatusDto> checkJobStatus(@PathVariable @NotNull UUID jobId){
        try{
            JobStatusDto jobStatusDto = jobService.getJobStatus(jobId);
            return ResponseEntity.ok(jobStatusDto);
        } catch (JobNotFoundException e){
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/problems/random")
    ResponseEntity<Problem> getRandomProblem(@RequestParam(required = false) String id){
        return ResponseEntity.ok(problemService.getRandomProblemExcludingId(id));
    }
}
