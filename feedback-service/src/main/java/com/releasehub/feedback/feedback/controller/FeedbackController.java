package com.releasehub.feedback.feedback.controller;

import com.releasehub.feedback.feedback.dto.CreateFeedbackRequest;
import com.releasehub.feedback.feedback.dto.FeedbackResponse;
import com.releasehub.feedback.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(@Valid @RequestBody CreateFeedbackRequest request) {
        return feedbackService.submit(request);
    }

    @GetMapping("/{softwareId}")
    public List<FeedbackResponse> findBySoftware(@PathVariable String softwareId) {
        if (softwareId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "softwareId must not be blank");
        }
        return feedbackService.findBySoftwareId(softwareId);
    }
}
