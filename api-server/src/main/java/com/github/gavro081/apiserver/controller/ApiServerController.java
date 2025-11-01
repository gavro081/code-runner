package com.github.gavro081.apiserver.controller;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.dto.JobStatusDto;
import com.github.gavro081.apiserver.exceptions.JobNotFoundException;
import com.github.gavro081.apiserver.service.JobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ApiServerController {
    private final JobService jobService;

    public ApiServerController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    String index(){
        return "api index";
    }

    @PostMapping("/submit")
    ResponseEntity<Map<String, UUID>> submitCode(@Valid @RequestBody CodeSubmissionDto codeSubmissionDto){
        UUID jobId = jobService.createJob(codeSubmissionDto);
        return ResponseEntity.accepted().body(Map.of("job_id", jobId));
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
}
