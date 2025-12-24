package com.github.gavro081.apiserver.dto;

public record ProblemSummaryDto(
        String title,
        String id,
        String difficulty
) {}
