package com.github.gavro081.apiserver.repository;

import com.github.gavro081.common.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobRepository extends MongoRepository<Job,UUID> {
}
