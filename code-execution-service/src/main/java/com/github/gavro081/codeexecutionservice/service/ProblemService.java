package com.github.gavro081.codeexecutionservice.service;

import com.github.gavro081.codeexecutionservice.models.TestCasesProjection;
import com.github.gavro081.codeexecutionservice.repository.ProblemRepository;
import org.springframework.stereotype.Service;

@Service
public class ProblemService {
    private final ProblemRepository problemRepository;

    public ProblemService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    public TestCasesProjection getTestCases(String id) {
        return problemRepository.findTestCasesById(id);
    }
}
