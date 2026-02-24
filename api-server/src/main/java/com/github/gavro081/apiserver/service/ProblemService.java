package com.github.gavro081.apiserver.service;


import com.github.gavro081.apiserver.dto.CreateProblemDto;
import com.github.gavro081.apiserver.dto.ProblemDto;
import com.github.gavro081.apiserver.dto.ProblemSummaryDto;
import com.github.gavro081.apiserver.repository.ProblemRepository;
import com.github.gavro081.common.model.Problem;
import com.github.gavro081.common.model.enums.ProgrammingLanguage;
import com.github.gavro081.common.model.TestCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProblemService {
    private final ProblemRepository problemRepository;

    public ProblemDto getProblemDto(String problemId){
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, String.format("Problem with ID %s not found", problemId)
                        )
                );

        return ProblemDto.builder()
                .title(problem.getTitle())
                .assumptions(problem.getAssumptions())
                .difficulty(problem.getDifficulty())
                .exampleTestCases(problem.getExampleTestCases())
                .description(problem.getDescription())
                .starterTemplates(problem.getStarterTemplates())
                .constraints(problem.getConstraints())
                .build();
    }

    public List<ProblemSummaryDto> getProblems(){
        return problemRepository.findAllAsSummaryDto();
    }

    public String getRandomProblemExcludingId(String id){
        return problemRepository.findRandomExceptId(id);
    }

    public void createProblem(@Valid CreateProblemDto problemDto) {
        String id = createIdFromTitle(problemDto.getTitle());

        if (problemRepository.existsById(id)){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Problem with this id already exists"
            );
        }

        Problem.ProblemBuilder problemBuilder = Problem.builder()
                .id(id)
                .title(problemDto.getTitle())
                .difficulty(problemDto.getDifficulty())
                .methodName(problemDto.getMethodName())
                .description(problemDto.getDescription())
                .assumptions(problemDto.getAssumptions())
                .constraints(problemDto.getConstraints());


        problemBuilder.exampleTestCases(
                problemDto
                        .getExampleTestCases()
                        .stream()
                        .map(tc -> new TestCase(tc.getInput(), tc.getExpectedOutput()))
                        .toList()
        );

        problemBuilder.testCases(
                problemDto
                        .getTestCases()
                        .stream()
                        .map(tc -> new TestCase(tc.getInput(),tc.getExpectedOutput()))
                        .toList()
        );

        Map<ProgrammingLanguage, String> templates = Map.of(
                ProgrammingLanguage.PYTHON, problemDto.getStarterTemplates().PYTHON,
                ProgrammingLanguage.JAVASCRIPT, problemDto.getStarterTemplates().JAVASCRIPT
        );

        problemBuilder.starterTemplates(templates);

        problemRepository.save(problemBuilder.build());
    }

    private String createIdFromTitle(String title) {
        return title.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]]","")
                .replaceAll("\\s+", "-");
    }
}
