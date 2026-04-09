package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recommendation_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", nullable = false)
    private RecommendationResult result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    @Column(name = "major_name", nullable = false, length = 200)
    private String majorName;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String reason;

    @Column(name = "career_paths", columnDefinition = "NVARCHAR(MAX)")
    private String careerPaths;             // JSON array

    @Column(name = "salary_range", length = 100)
    private String salaryRange;

    @Column(name = "skills_to_improve", columnDefinition = "NVARCHAR(MAX)")
    private String skillsToImprove;         // JSON array

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
