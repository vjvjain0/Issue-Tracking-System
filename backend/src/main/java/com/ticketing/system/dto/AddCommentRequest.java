package com.ticketing.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddCommentRequest {
    @NotBlank(message = "Comment content is required")
    private String content;
}
