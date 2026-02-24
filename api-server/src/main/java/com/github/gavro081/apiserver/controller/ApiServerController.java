package com.github.gavro081.apiserver.controller;

import com.github.gavro081.apiserver.dto.*;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.service.JobService;
import com.github.gavro081.apiserver.service.ProblemService;
import com.github.gavro081.common.model.Problem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/problems")
public class ApiServerController {
    private final JobService jobService;
    private final ProblemService problemService;

    public ApiServerController(JobService jobService, ProblemService problemService) {
        this.jobService = jobService;
        this.problemService = problemService;
    }

    @PostMapping("/submit")
    ResponseEntity<Map<String, UUID>> submitCode(@Valid @RequestBody CodeSubmissionDto codeSubmissionDto){
        UUID jobId = jobService.createJob(codeSubmissionDto);
        return ResponseEntity.accepted().body(Map.of("job_id", jobId));
    }

    @GetMapping("/{problemId}")
    ResponseEntity<ProblemDto> getProblem(@PathVariable @NotNull String problemId){
        return ResponseEntity.ok(problemService.getProblemDto(problemId));
    }

    @GetMapping()
    ResponseEntity<List<ProblemSummaryDto>> getProblems(){
        return ResponseEntity.ok(problemService.getProblems());
    }

    @GetMapping("/status/{jobId}")
    ResponseEntity<JobStatusDto> checkJobStatus(@PathVariable @NotNull UUID jobId){
        return ResponseEntity.ok(jobService.getJobStatus(jobId));
    }

    @GetMapping("/random")
    ResponseEntity<String> getRandomProblem(@RequestParam(required = false) String id){
        return ResponseEntity.ok(problemService.getRandomProblemExcludingId(id));
    }

    @PostMapping()
    public ResponseEntity<Void> createProblem(@Valid @RequestBody CreateProblemDto problemDto){
        problemService.createProblem(problemDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
