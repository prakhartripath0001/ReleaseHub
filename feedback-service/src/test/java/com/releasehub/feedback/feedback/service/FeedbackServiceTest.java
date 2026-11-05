package com.releasehub.feedback.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.releasehub.feedback.catalog.VersionLookupRepository;
import com.releasehub.feedback.feedback.dto.CreateFeedbackRequest;
import com.releasehub.feedback.feedback.entity.FeedbackEntity;
import com.releasehub.feedback.feedback.repository.FeedbackRepository;
import com.releasehub.feedback.outbox.OutboxEventEntity;
import com.releasehub.feedback.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @Mock
    private VersionLookupRepository versionLookupRepository;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(
                feedbackRepository,
                outboxRepository,
                versionLookupRepository,
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void submitPersistsFeedbackAndMatchingOutboxEvent() {
        String versionId = UUID.randomUUID().toString();
        String softwareId = UUID.randomUUID().toString();
        CreateFeedbackRequest request = new CreateFeedbackRequest(versionId, UUID.randomUUID().toString(), 5, "Excellent");
        when(versionLookupRepository.findSoftwareId(versionId)).thenReturn(softwareId);
        when(feedbackRepository.saveAndFlush(any(FeedbackEntity.class))).thenAnswer(invocation -> {
            FeedbackEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID().toString());
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        });

        var response = service.submit(request);

        assertThat(response.softwareId()).isEqualTo(softwareId);
        assertThat(response.rating()).isEqualTo(5);
        ArgumentCaptor<OutboxEventEntity> event = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo("feedback.submitted");
        assertThat(event.getValue().getPayload()).contains(response.id(), softwareId);
    }

    @Test
    void submitRejectsDuplicateFeedbackBeforeWritingOutbox() {
        String versionId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        when(versionLookupRepository.findSoftwareId(versionId)).thenReturn(UUID.randomUUID().toString());
        when(feedbackRepository.existsByUserIdAndVersionId(userId, versionId)).thenReturn(true);

        assertThatThrownBy(() -> service.submit(new CreateFeedbackRequest(versionId, userId, 4, null)))
                .isInstanceOf(DuplicateFeedbackException.class);

        verify(feedbackRepository, never()).saveAndFlush(any(FeedbackEntity.class));
        verify(outboxRepository, never()).save(any(OutboxEventEntity.class));
    }
}
