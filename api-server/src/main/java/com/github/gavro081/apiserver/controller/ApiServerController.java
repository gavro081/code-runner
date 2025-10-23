package com.github.gavro081.apiserver.controller;

import com.github.gavro081.apiserver.dto.CodeSubmissionDto;
import com.github.gavro081.apiserver.model.ProgrammingLanguage;
import com.github.gavro081.apiserver.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
