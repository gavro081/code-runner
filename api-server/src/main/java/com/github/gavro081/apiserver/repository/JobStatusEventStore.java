package com.github.gavro081.apiserver.repository;

import com.github.gavro081.common.events.JobStatusEvent;
import com.github.gavro081.common.model.Job;

import java.util.Optional;
import java.util.UUID;

public interface JobStatusEventStore {
    void save(JobStatusEvent jobStatusEvent);
    Optional<JobStatusEvent> findById(UUID uuid);
}
