package com.github.gavro081.apiserver.dto;

import com.github.gavro081.common.model.ProgrammingLanguage;
import com.github.gavro081.common.model.TestCase;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ProblemDto (
    String difficulty,
    String title,
    String description,
    List<TestCase> exampleTestCases,
    List<String> assumptions,
    List<String> constraints,
    Map<ProgrammingLanguage, String> starterTemplates
){}
