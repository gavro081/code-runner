package com.github.gavro081.apiserver.dto;

import com.github.gavro081.common.model.Job;
import com.github.gavro081.common.model.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record JobStatusDto(
    @NotNull @NotBlank JobStatus jobStatus,
   String stdout,
   String stderr
) {}
