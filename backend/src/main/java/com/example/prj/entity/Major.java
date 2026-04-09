package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "major")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id")
    private Field field;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 50)
    private String code;                    // Tự sinh hoặc để null ban đầu

    private String url;
    private String imageUrl;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String target;                  // Mô tả mục tiêu ngành

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String careerOpportunities;

    // Các trường hỗ trợ AI Recommendation
    @Builder.Default
    private Integer hotTrendScore = 5;          // 1-10
    @Builder.Default
    private Integer aiResistanceScore = 5;      // Kháng AI tự động hóa (1-10)
    private String salaryRange;                 // Ví dụ: "8-15 triệu", "15-30 triệu"

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
