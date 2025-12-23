package com.github.gavro081.apiserver.service;


import com.github.gavro081.apiserver.repository.ProblemRepository;
import com.github.gavro081.common.model.Problem;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProblemService {
    private final ProblemRepository problemRepository;
    private final MongoTemplate mongoTemplate;

    public ProblemService(ProblemRepository problemRepository, MongoTemplate mongoTemplate) {
        this.problemRepository = problemRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Problem getProblem(String problemId){
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException(String.format("Problem with ID %s not found", problemId)));
    }

    public List<Problem> getProblems(){
        return problemRepository.findAll();
    }

    public Problem getRandomProblemExcludingId(String id){
        return problemRepository.findRandomExceptId(id);
    }
}
