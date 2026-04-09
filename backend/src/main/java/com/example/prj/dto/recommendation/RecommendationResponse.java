package com.example.prj.dto.recommendation;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendationResponse(
        Long id,
        Long submissionId,
        String studentName,
        String summary,
        String modelUsed,
        Long processingTimeMs,
        LocalDateTime createdAt,
        List<RecommendationItemResponse> recommendations
) {}
