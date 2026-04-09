package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recommendation_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private SurveySubmission submission;

    @Column(name = "ai_summary", columnDefinition = "NVARCHAR(MAX)")
    private String aiSummary;

    @Column(name = "ai_raw_response", columnDefinition = "NVARCHAR(MAX)")
    private String aiRawResponse;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "result", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecommendationItem> items = new ArrayList<>();
}
