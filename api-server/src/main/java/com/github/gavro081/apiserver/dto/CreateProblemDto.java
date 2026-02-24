package com.github.gavro081.apiserver.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProblemDto {

    @NotBlank(message = "title is required")
    private String title;

    @NotNull(message = "difficulty is required")
    @Pattern(regexp = "EASY|MEDIUM|HARD", message = "difficulty must be EASY, MEDIUM, or HARD")
    private String difficulty;

    @NotBlank(message = "method name is required")
    private String methodName;

    @NotBlank(message = "description is required")
    private String description;

    // optional
    private List<String> assumptions;
    private List<String> constraints;

    @NotNull(message = "example test cases are required")
    @Size(min = 1, message = "at least one example test case is required")
    @Valid
    private List<TestCaseDto> exampleTestCases;

    @NotNull(message = "test cases are required")
    @Size(min = 1, message = "at least one test case is required")
    @Valid
    private List<TestCaseDto> testCases;

    @NotNull(message = "starter templates are required")
    @Valid
    public StarterTemplatesDto starterTemplates;

    @Data
    public static class TestCaseDto {
        @NotBlank(message = "test case input is required")
        private String input;

        @NotBlank(message = "test case expected output is required")
        private String expectedOutput;
    }

    @Data
    public static class StarterTemplatesDto {
        @JsonProperty("PYTHON")
        public String PYTHON;

        @JsonProperty("JAVASCRIPT")
        public String JAVASCRIPT;
    }
}
