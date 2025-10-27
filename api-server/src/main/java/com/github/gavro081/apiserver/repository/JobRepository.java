package com.github.gavro081.apiserver.repository;

import com.github.gavro081.common.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job,UUID> {
}
