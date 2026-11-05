package com.analytics_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventListener {

    private final AnalyticsEventProcessor processor;

    public AnalyticsEventListener(AnalyticsEventProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(topics = "feedback.submitted")
    public void onFeedbackSubmitted(String payload) {
        processor.processFeedback(payload);
    }

    @KafkaListener(topics = "download.completed")
    public void onDownloadCompleted(String payload) {
        processor.processDownload(payload);
    }
}
