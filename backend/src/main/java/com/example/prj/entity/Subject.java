package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String description;

    private Integer semester;                   // 1,2,3,4,5,6

    private String category;                    // "Chuyên môn", "Kỹ năng mềm", "AI tools", "Ngoại ngữ"...

    @Builder.Default
    private Boolean isCore = true;
}
