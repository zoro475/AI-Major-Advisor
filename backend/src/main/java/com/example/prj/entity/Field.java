package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "field")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Field {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;                    // Ví dụ: Công nghệ Thông tin, Thiết kế & Sáng tạo, Ngoại ngữ...

    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
