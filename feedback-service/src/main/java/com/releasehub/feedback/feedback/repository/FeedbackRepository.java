package com.releasehub.feedback.feedback.repository;

import com.releasehub.feedback.feedback.entity.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, String> {

    boolean existsByUserIdAndVersionId(String userId, String versionId);

    List<FeedbackEntity> findBySoftwareIdOrderByCreatedAtDesc(String softwareId);
}
