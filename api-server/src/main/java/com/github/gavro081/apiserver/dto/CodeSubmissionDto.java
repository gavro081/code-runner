package com.github.gavro081.apiserver.dto;

import com.github.gavro081.apiserver.model.ProgrammingLanguage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CodeSubmissionDto(
    @NotBlank(message = "Code cannot be empty")
    @Size(max = 10000, message = "Code cannot be larger than 10KB")
    String code,
    @NotNull(message = "Language is required")
    ProgrammingLanguage language
){}
