package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.config.IssuesApiValidationProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.UpdateIssueRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssuesRequestValidator {

    private static final String REQUEST_VALIDATION_FAILED = "Request validation failed";

    private final IssuesApiProperties issuesApiProperties;

    public void validateCreateRequest(final CreateIssueRequest request) {
        final List<String> details = new ArrayList<>();

        validateRequiredTitle(request.title(), details);
        validateDescription(request.description(), details);
        validateLabels("labels", request.labels(), details);

        throwIfInvalid(details);
    }

    public void validateUpdateRequest(final UpdateIssueRequest request) {
        final List<String> details = new ArrayList<>();

        if (!hasEffectiveChange(request)) {
            details.add(
                    "request must include at least one update field: title, description, addLabels, or removeLabels");
        }

        validateOptionalTitle(request.title(), details);
        validateDescription(request.description(), details);
        validateLabels("addLabels", request.addLabels(), details);
        validateLabels("removeLabels", request.removeLabels(), details);
        validateOverlappingLabels(request.addLabels(), request.removeLabels(), details);

        throwIfInvalid(details);
    }

    private boolean hasEffectiveChange(final UpdateIssueRequest request) {
        return request.title() != null
                || request.description() != null
                || hasAnyItems(request.addLabels())
                || hasAnyItems(request.removeLabels());
    }

    private boolean hasAnyItems(final List<String> labels) {
        return labels != null && !labels.isEmpty();
    }

    private void validateRequiredTitle(final String title, final List<String> details) {
        if (title == null || title.isBlank()) {
            details.add("title must not be blank");
            return;
        }

        validateTitleLength(title, details);
    }

    private void validateOptionalTitle(final String title, final List<String> details) {
        if (title == null) {
            return;
        }

        if (title.isBlank()) {
            details.add("title must not be blank");
            return;
        }

        validateTitleLength(title, details);
    }

    private void validateTitleLength(final String title, final List<String> details) {
        final IssuesApiValidationProperties validation = issuesApiProperties.validation();
        final int titleLength = title.length();
        if (titleLength < validation.titleMinLength() || titleLength > validation.titleMaxLength()) {
            details.add("title length must be between " + validation.titleMinLength() + " and "
                    + validation.titleMaxLength());
        }
    }

    private void validateDescription(final String description, final List<String> details) {
        if (description == null) {
            return;
        }

        final int descriptionMaxLength = issuesApiProperties.validation().descriptionMaxLength();
        if (description.length() > descriptionMaxLength) {
            details.add("description length must be less than or equal to " + descriptionMaxLength);
        }
    }

    private void validateLabels(final String fieldName, final List<String> labels, final List<String> details) {
        if (labels == null) {
            return;
        }

        final int maxLabels = issuesApiProperties.validation().maxLabelsPerRequestField();
        if (labels.size() > maxLabels) {
            details.add(fieldName + " must contain at most " + maxLabels + " items");
        }

        for (int index = 0; index < labels.size(); index++) {
            final String label = labels.get(index);
            if (label == null) {
                details.add(fieldName + "[" + index + "] must not be null");
                continue;
            }

            if (label.isBlank()) {
                details.add(fieldName + "[" + index + "] must not be blank");
            }
        }
    }

    private void validateOverlappingLabels(
            final List<String> addLabels, final List<String> removeLabels, final List<String> details) {
        if (addLabels == null || addLabels.isEmpty() || removeLabels == null || removeLabels.isEmpty()) {
            return;
        }

        final Set<String> removeLabelSet = new HashSet<>(removeLabels);
        final List<String> overlaps = addLabels.stream()
                .filter(label -> label != null && removeLabelSet.contains(label))
                .distinct()
                .toList();

        if (!overlaps.isEmpty()) {
            details.add("addLabels and removeLabels must not overlap: " + overlaps);
        }
    }

    private void throwIfInvalid(final List<String> details) {
        if (!details.isEmpty()) {
            throw new ValidationException(REQUEST_VALIDATION_FAILED, details);
        }
    }
}
