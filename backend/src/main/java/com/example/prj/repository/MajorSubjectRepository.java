package com.example.prj.repository;

import com.example.prj.entity.MajorSubject;
import com.example.prj.entity.MajorSubjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MajorSubjectRepository extends JpaRepository<MajorSubject, MajorSubjectId> {
}
