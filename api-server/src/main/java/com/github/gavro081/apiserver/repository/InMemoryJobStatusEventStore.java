package com.github.gavro081.apiserver.repository;

import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.JobStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryJobStatusEventStore implements JobStatusEventStore {
    private final ConcurrentHashMap<UUID, JobStatusEvent> jobMap = new ConcurrentHashMap<>();

    @Override
    public void save(JobStatusEvent job) {
        jobMap.put(job.jobId(), job);
    }

    @Override
    public Optional<JobStatusEvent> findById(UUID uuid) {
        Optional<JobStatusEvent> jobStatusEvent = Optional.ofNullable(jobMap.get(uuid));
        if (jobStatusEvent.isPresent() &&
            (jobStatusEvent.get().status() == JobStatus.COMPLETED ||
            jobStatusEvent.get().status() == JobStatus.FAILED)
        ){
            jobMap.remove(uuid);
        }
        return jobStatusEvent;
    }
}
