package com.gitlabflow.floworchestrator.common.web;

import java.util.List;
import lombok.Builder;

@Builder
public record ErrorResponse(String code, String message, List<String> details) {
    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
