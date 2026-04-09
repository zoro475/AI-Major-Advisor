package com.example.prj.repository;

import com.example.prj.entity.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    @Query("SELECT r FROM RecommendationResult r LEFT JOIN FETCH r.items WHERE r.submission.id = :submissionId")
    Optional<RecommendationResult> findBySubmissionIdWithItems(@Param("submissionId") Long submissionId);

    boolean existsBySubmissionId(Long submissionId);
}
