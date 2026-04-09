package com.example.prj.dto.recommendation;

import java.util.List;

public record RecommendationItemResponse(
        Long majorId,
        String majorName,
        String fieldName,
        Integer matchScore,
        String reason,
        List<String> careerPaths,
        String salaryRange,
        List<String> skillsToImprove
) {}
