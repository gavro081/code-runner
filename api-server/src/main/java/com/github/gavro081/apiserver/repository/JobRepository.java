package com.github.gavro081.apiserver.repository;

import com.github.gavro081.apiserver.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobRepository extends JpaRepository<Job,UUID> {
}
