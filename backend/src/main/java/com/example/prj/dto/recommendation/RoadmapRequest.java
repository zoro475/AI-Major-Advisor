package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record RoadmapRequest(
        @NotNull Long submissionId,
        @NotNull Long majorId
) {}
