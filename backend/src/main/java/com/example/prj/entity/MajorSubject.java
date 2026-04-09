package com.example.prj.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "major_subject")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MajorSubject {

    @EmbeddedId
    private MajorSubjectId id;

    @ManyToOne
    @MapsId("majorId")
    private Major major;

    @ManyToOne
    @MapsId("subjectId")
    private Subject subject;
}

