package com.example.prj.repository;

import com.example.prj.entity.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {
    // Thường không cần query trực tiếp - đã có cascade từ Submission
}
