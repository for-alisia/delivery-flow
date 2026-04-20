package com.gitlabflow.floworchestrator.orchestration.issues.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import com.gitlabflow.floworchestrator.common.error.ValidationException;
import com.gitlabflow.floworchestrator.config.IssuesApiProperties;
import com.gitlabflow.floworchestrator.config.IssuesApiValidationProperties;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.CreateIssueRequest;
import com.gitlabflow.floworchestrator.orchestration.issues.rest.dto.UpdateIssueRequest;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuesRequestValidatorTest {

    private final IssuesRequestValidator validator = new IssuesRequestValidator(
            new IssuesApiProperties(20, 40, new IssuesApiValidationProperties(3, 10, 20, 2)));

    @Test
    @DisplayName("accepts create request at title lower bound")
    void acceptsCreateRequestAtTitleLowerBound() {
        final CreateIssueRequest request = new CreateIssueRequest("abc", "description", List.of("bug"));

        assertThatCode(() -> validator.validateCreateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects create request with blank title")
    void rejectsCreateRequestWithBlankTitle() {
        final CreateIssueRequest request = new CreateIssueRequest("   ", null, null);

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("title must not be blank");
    }

    @Test
    @DisplayName("rejects create request when title length is outside configured range")
    void rejectsCreateRequestWhenTitleLengthIsOutsideConfiguredRange() {
        final CreateIssueRequest request = new CreateIssueRequest("ab", null, null);

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("title length must be between 3 and 10");
    }

    @Test
    @DisplayName("rejects create request when description exceeds configured maximum")
    void rejectsCreateRequestWhenDescriptionExceedsConfiguredMaximum() {
        final CreateIssueRequest request = new CreateIssueRequest("deploy", "x".repeat(21), null);

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("description length must be less than or equal to 20");
    }

    @Test
    @DisplayName("rejects create request when labels exceed configured per field limit")
    void rejectsCreateRequestWhenLabelsExceedConfiguredPerFieldLimit() {
        final CreateIssueRequest request = new CreateIssueRequest("deploy", null, List.of("bug", "infra", "backend"));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("labels must contain at most 2 items");
    }

    @Test
    @DisplayName("rejects create request with blank label values")
    void rejectsCreateRequestWithBlankLabelValues() {
        final CreateIssueRequest request = new CreateIssueRequest("deploy", null, List.of("bug", " "));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("labels[1] must not be blank");
    }

    @Test
    @DisplayName("rejects create request with null label values")
    void rejectsCreateRequestWithNullLabelValues() {
        final CreateIssueRequest request = new CreateIssueRequest("deploy", null, Arrays.asList("bug", null));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateCreateRequest(request));

        assertThat(exception.details()).containsExactly("labels[1] must not be null");
    }

    @Test
    @DisplayName("accepts update request with empty description as effective change")
    void acceptsUpdateRequestWithEmptyDescriptionAsEffectiveChange() {
        final UpdateIssueRequest request = new UpdateIssueRequest(null, "", null, null);

        assertThatCode(() -> validator.validateUpdateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects update request when payload has no effective fields")
    void rejectsUpdateRequestWhenPayloadHasNoEffectiveFields() {
        final UpdateIssueRequest request = new UpdateIssueRequest(null, null, List.of(), List.of());

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details())
                .containsExactly(
                        "request must include at least one update field: title, description, addLabels, or removeLabels");
    }

    @Test
    @DisplayName("rejects update request with blank title")
    void rejectsUpdateRequestWithBlankTitle() {
        final UpdateIssueRequest request = new UpdateIssueRequest(" ", null, null, null);

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details()).containsExactly("title must not be blank");
    }

    @Test
    @DisplayName("rejects update request when title exceeds configured maximum")
    void rejectsUpdateRequestWhenTitleExceedsConfiguredMaximum() {
        final UpdateIssueRequest request = new UpdateIssueRequest("x".repeat(11), null, null, null);

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details()).containsExactly("title length must be between 3 and 10");
    }

    @Test
    @DisplayName("rejects update request when labels contain invalid values")
    void rejectsUpdateRequestWhenLabelsContainInvalidValues() {
        final UpdateIssueRequest request = new UpdateIssueRequest(null, null, Arrays.asList("bug", null), List.of(" "));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details())
                .containsExactly("addLabels[1] must not be null", "removeLabels[0] must not be blank");
    }

    @Test
    @DisplayName("rejects update request when labels exceed configured limit")
    void rejectsUpdateRequestWhenLabelsExceedConfiguredLimit() {
        final UpdateIssueRequest request =
                new UpdateIssueRequest(null, null, List.of("a", "b", "c"), List.of("x", "y", "z"));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details())
                .containsExactly("addLabels must contain at most 2 items", "removeLabels must contain at most 2 items");
    }

    @Test
    @DisplayName("rejects update request when add and remove labels overlap")
    void rejectsUpdateRequestWhenAddAndRemoveLabelsOverlap() {
        final UpdateIssueRequest request = new UpdateIssueRequest(null, null, List.of("bug", "infra"), List.of("bug"));

        final ValidationException exception =
                assertThrowsExactly(ValidationException.class, () -> validator.validateUpdateRequest(request));

        assertThat(exception.details()).containsExactly("addLabels and removeLabels must not overlap: [bug]");
    }
}
