package com.github.gavro081.codeexecutionservice.models;

import com.github.gavro081.common.model.TestCase;
import lombok.Getter;

import java.util.List;

@Getter
public class ProblemTestDto {
    private List<TestCase> testCases;
    private String template;
}
