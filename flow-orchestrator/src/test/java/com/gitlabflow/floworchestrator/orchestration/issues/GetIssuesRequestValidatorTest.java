package com.gitlabflow.floworchestrator.orchestration.issues;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.models.GetIssuesRequest;

@DisplayName("GetIssuesRequestValidator")
class GetIssuesRequestValidatorTest {

    private final GetIssuesRequestValidator validator = new GetIssuesRequestValidator();

    @Test
    @DisplayName("given supported state when validate then passes")
    void givenSupportedStateWhenValidateThenPasses() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, "opened", null, null, null, null, null, null);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given invalid state when validate then throws validation exception")
    void givenInvalidStateWhenValidateThenThrowsValidationException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, "invalid", null, null, null, null, null, null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("state");
    }

    @Test
    @DisplayName("given invalid order by when validate then throws validation exception")
    void givenInvalidOrderByWhenValidateThenThrowsValidationException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, "invalid", null, null, null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("orderBy");
    }

    @Test
    @DisplayName("given invalid sort when validate then throws validation exception")
    void givenInvalidSortWhenValidateThenThrowsValidationException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, "invalid", null, null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("sort");
    }

    @Test
    @DisplayName("given non positive page when validate then throws validation exception")
    void givenNonPositivePageWhenValidateThenThrowsValidationException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, 0, null);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("page");
    }

    @Test
    @DisplayName("given out of range per page when validate then throws validation exception")
    void givenOutOfRangePerPageWhenValidateThenThrowsValidationException() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, 101);

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("perPage");
    }

    @Test
    @DisplayName("given all null when validate then passes")
    void givenAllNullWhenValidateThenPasses() {
        GetIssuesRequest request = new GetIssuesRequest(null, null, null, null, null, null, null, null, null, null);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given valid paging and sort values when validate then passes")
    void givenValidPagingAndSortValuesWhenValidateThenPasses() {
        GetIssuesRequest request = new GetIssuesRequest(
                null,
                null,
                null,
                "all",
                null,
                null,
                "created_at",
                "desc",
                1,
                100
        );

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }
}
