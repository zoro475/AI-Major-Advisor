package com.example.prj.dto.recommendation;

import java.util.List;

public record HybridCareerResponse(
        String major1Name,
        String major2Name,
        List<HybridCareer> hybridCareers,
        String aiSummary
) {
    public record HybridCareer(
            String careerTitle,
            String description,
            int demandScore,
            String salaryRange,
            List<String> requiredSkills,
            List<String> companies,
            String growthOutlook
    ) {}
}
