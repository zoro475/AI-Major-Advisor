package com.example.prj.repository;

import com.example.prj.entity.SurveySubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SurveySubmissionRepository extends JpaRepository<SurveySubmission, Long> {

    List<SurveySubmission> findByStudentEmailOrderBySubmittedAtDesc(String studentEmail);

    @Query("SELECT s FROM SurveySubmission s LEFT JOIN FETCH s.answers a LEFT JOIN FETCH a.question WHERE s.id = :id")
    Optional<SurveySubmission> findByIdWithAnswers(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM SurveySubmission s LEFT JOIN FETCH s.answers a LEFT JOIN FETCH a.question WHERE s.studentEmail = :email ORDER BY s.submittedAt DESC")
    List<SurveySubmission> findByEmailWithAnswers(@Param("email") String email);
}
