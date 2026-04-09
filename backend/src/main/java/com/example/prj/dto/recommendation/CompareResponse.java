package com.example.prj.dto.recommendation;

import java.util.List;

public record CompareResponse(
        Long submissionId,
        String studentName,
        MajorCompareDetail major1,
        MajorCompareDetail major2,
        String aiConclusion,          // AI kết luận ngành nào phù hợp hơn
        String conclusionMajorName,   // Tên ngành AI recommend
        Long conclusionMajorId        // ID ngành AI recommend
) {
    public record MajorCompareDetail(
            Long majorId,
            String majorName,
            String fieldName,
            Integer matchScore,
            String reason,
            List<String> careerPaths,
            String salaryRange,
            Integer hotTrendScore,
            Integer aiResistanceScore,
            List<String> keySubjects,        // Môn học chính
            List<String> skillsToImprove,
            String studyDuration,            // Thời gian học
            String admissionRequirements     // Yêu cầu đầu vào
    ) {}
}
