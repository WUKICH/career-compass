package com.coze.aicoze.dto;

import com.coze.aicoze.dto.FeedbackRequest;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeedbackRequest {
    private String sessionId;
    private List<FeedbackItem> feedbackItems;

    @Data
    public static class FeedbackItem{
        private Integer questionNumber;
        private String answer;
    }
}

