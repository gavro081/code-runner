package com.github.gavro081.apiserver.service;


import com.github.gavro081.apiserver.dto.ProblemSummaryDto;
import com.github.gavro081.apiserver.repository.ProblemRepository;
import com.github.gavro081.common.model.Problem;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;

    public Problem getProblem(String problemId){
        return problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException(String.format("Problem with ID %s not found", problemId)));
    }

    public List<ProblemSummaryDto> getProblems(){
        return problemRepository.findAllAsSummaryDto();
    }

    public String getRandomProblemExcludingId(String id){
        return problemRepository.findRandomExceptId(id);
    }
}
