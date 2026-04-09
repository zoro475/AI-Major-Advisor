package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record RecommendationRequest(
        @NotNull(message = "submissionId không được để trống")
        Long submissionId
) {}
