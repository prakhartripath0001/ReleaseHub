package com.releasehub.feedback.feedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasehub.feedback.catalog.VersionLookupRepository;
import com.releasehub.feedback.feedback.dto.CreateFeedbackRequest;
import com.releasehub.feedback.feedback.dto.FeedbackResponse;
import com.releasehub.feedback.feedback.entity.FeedbackEntity;
import com.releasehub.feedback.feedback.event.FeedbackSubmittedEvent;
import com.releasehub.feedback.feedback.repository.FeedbackRepository;
import com.releasehub.feedback.outbox.OutboxEventEntity;
import com.releasehub.feedback.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final OutboxEventRepository outboxRepository;
    private final VersionLookupRepository versionLookupRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           OutboxEventRepository outboxRepository,
                           VersionLookupRepository versionLookupRepository,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.feedbackRepository = feedbackRepository;
        this.outboxRepository = outboxRepository;
        this.versionLookupRepository = versionLookupRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public FeedbackResponse submit(CreateFeedbackRequest request) {
        String softwareId;
        try {
            softwareId = versionLookupRepository.findSoftwareId(request.versionId());
        } catch (EmptyResultDataAccessException ex) {
            throw new FeedbackNotFoundException("Version not found: " + request.versionId());
        }

        if (feedbackRepository.existsByUserIdAndVersionId(request.userId(), request.versionId())) {
            throw new DuplicateFeedbackException("User has already submitted feedback for this version");
        }

        FeedbackEntity entity = new FeedbackEntity();
        entity.setVersionId(request.versionId());
        entity.setSoftwareId(softwareId);
        entity.setUserId(request.userId());
        entity.setRating((short) request.rating());
        entity.setComment(request.comment());
        FeedbackEntity saved;
        try {
            saved = feedbackRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("uq_feedback_user_version")) {
                throw new DuplicateFeedbackException("User has already submitted feedback for this version", ex);
            }
            throw ex;
        }

        FeedbackSubmittedEvent event = new FeedbackSubmittedEvent(
                saved.getId(),
                saved.getVersionId(),
                saved.getSoftwareId(),
                saved.getUserId(),
                saved.getRating(),
                saved.getComment(),
                saved.getCreatedAt());
        try {
            outboxRepository.save(new OutboxEventEntity(
                    saved.getSoftwareId(),
                    "feedback.submitted",
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize feedback event " + saved.getId(), ex);
        }
        Counter.builder("releasehub.feedback.submissions")
                .description("Successfully submitted feedback")
                .register(meterRegistry)
                .increment();
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> findBySoftwareId(String softwareId) {
        return feedbackRepository.findBySoftwareIdOrderByCreatedAtDesc(softwareId).stream()
                .map(this::toResponse)
                .toList();
    }

    private FeedbackResponse toResponse(FeedbackEntity entity) {
        return new FeedbackResponse(
                entity.getId(),
                entity.getVersionId(),
                entity.getSoftwareId(),
                entity.getUserId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt());
    }
}
