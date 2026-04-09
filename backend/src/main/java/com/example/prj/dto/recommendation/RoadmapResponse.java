package com.example.prj.dto.recommendation;

import java.util.List;

public record RoadmapResponse(
        Long majorId,
        String majorName,
        String overview,                          // Tóm tắt lộ trình
        List<RoadmapPhase> phases,               // Các giai đoạn phát triển
        List<String> certifications,              // Chứng chỉ gợi ý
        List<CareerBranch> careerBranches         // Các mảng việc làm
) {
    public record RoadmapPhase(
            String title,                         // "Năm 1-2: Fresher"
            String period,                        // "0-2 năm"
            String description,                   // Mô tả giai đoạn
            List<String> skills,                  // Kỹ năng cần có
            String salaryRange,                   // Mức lương giai đoạn này
            List<String> positions                // Vị trí có thể ứng tuyển
    ) {}

    public record CareerBranch(
            String name,                          // "Frontend Developer"
            String description,                   // Mô tả mảng
            List<String> tools,                   // Công cụ/Công nghệ
            String demandLevel                    // "Rất cao" / "Cao" / "Trung bình"
    ) {}
}
