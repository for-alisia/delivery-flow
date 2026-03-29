package com.gitlabflow.floworchestrator.orchestration.issues;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gitlabflow.floworchestrator.common.errors.ValidationException;
import com.gitlabflow.floworchestrator.orchestration.issues.models.ListProjectIssuesQuery;

@DisplayName("ListProjectIssuesRequestValidator")
class ListProjectIssuesRequestValidatorTest {

    private final ListProjectIssuesRequestValidator validator = new ListProjectIssuesRequestValidator();

    @Test
    @DisplayName("given valid project and paging when validate then passes")
    void givenValidProjectAndPagingWhenValidateThenPasses() {
        ListProjectIssuesQuery query = new ListProjectIssuesQuery("123", 1, 20);

        assertThatCode(() -> validator.validate(query)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("given blank project id when validate then throws validation exception")
    void givenBlankProjectIdWhenValidateThenThrowsValidationException() {
        ListProjectIssuesQuery query = new ListProjectIssuesQuery("  ", 1, 20);

        assertThatThrownBy(() -> validator.validate(query))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("projectId");
    }

    @Test
    @DisplayName("given non positive page when validate then throws validation exception")
    void givenNonPositivePageWhenValidateThenThrowsValidationException() {
        ListProjectIssuesQuery query = new ListProjectIssuesQuery("123", 0, 20);

        assertThatThrownBy(() -> validator.validate(query))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("page");
    }

    @Test
    @DisplayName("given non positive page size when validate then throws validation exception")
    void givenNonPositivePageSizeWhenValidateThenThrowsValidationException() {
        ListProjectIssuesQuery query = new ListProjectIssuesQuery("123", 1, 0);

        assertThatThrownBy(() -> validator.validate(query))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("pageSize");
    }

    @Test
    @DisplayName("given page size greater than one hundred when validate then throws validation exception")
    void givenPageSizeGreaterThanOneHundredWhenValidateThenThrowsValidationException() {
        ListProjectIssuesQuery query = new ListProjectIssuesQuery("123", 1, 101);

        assertThatThrownBy(() -> validator.validate(query))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("pageSize");
    }
}
