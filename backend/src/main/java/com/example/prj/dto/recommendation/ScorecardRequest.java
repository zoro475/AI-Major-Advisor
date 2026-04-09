package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record ScorecardRequest(
        @NotNull Long submissionId,
        @NotNull Long majorId
) {}
