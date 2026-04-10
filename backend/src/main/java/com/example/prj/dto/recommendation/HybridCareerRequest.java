package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record HybridCareerRequest(
        @NotNull Long submissionId,
        @NotNull Long majorId1,
        @NotNull Long majorId2
) {}
