package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.codeexecutionservice.exceptions.JobNotFoundException;
import com.github.gavro081.codeexecutionservice.repository.JobHandlerRepository;
import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobHandlerService {
    private final JobHandlerRepository repository;

    public JobHandlerService(JobHandlerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Job markAndGetJob(UUID uuid) {
        Job job = repository.findAndLockBId(uuid)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + uuid));
        if (job.getStatus() != JobStatus.PENDING){
            // other worker already picked up job
            return null;
        }
        job.setStatus(JobStatus.RUNNING);
        return repository.save(job);
    }

    @Transactional
    public void finalizeJob(UUID uuid, JobStatus newStatus, String stdout, String stderr){
        Job job = repository.findById(uuid)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + uuid));
        job.setStatus(newStatus);
        job.setStdout(stdout);
        job.setStderr(stderr);
        job.setCompletedAt(Instant.now());
        repository.save(job);
    }
}
