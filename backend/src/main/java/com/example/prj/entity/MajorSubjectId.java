package com.example.prj.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

// Class Id composite
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MajorSubjectId implements Serializable {
    private Long majorId;
    private Long subjectId;
}
