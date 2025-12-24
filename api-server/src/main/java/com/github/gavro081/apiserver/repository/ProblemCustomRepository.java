package com.github.gavro081.apiserver.repository;

import com.github.gavro081.apiserver.dto.ProblemSummaryDto;

import java.util.List;

public interface ProblemCustomRepository {
    String findRandomExceptId(String id);
    List<ProblemSummaryDto> findAllAsSummaryDto();
}
