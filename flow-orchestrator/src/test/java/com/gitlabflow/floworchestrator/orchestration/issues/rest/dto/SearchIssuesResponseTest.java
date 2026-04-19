package com.gitlabflow.floworchestrator.orchestration.issues.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlabflow.floworchestrator.orchestration.common.model.ChangeField;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.LabelChangeSetDto;
import com.gitlabflow.floworchestrator.orchestration.common.rest.dto.UserDto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchIssuesResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("normalizes null items to empty immutable list")
    void normalizesNullItemsToEmptyImmutableList() {
        final SearchIssuesResponse response = new SearchIssuesResponse(null, 0, 1);
        final List<IssueSummaryDto> responseItems = response.items();

        assertThat(responseItems).isEmpty();
        assertThatThrownBy(() -> responseItems.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("defensively copies and detaches from mutable input list")
    void defensivelyCopiesAndDetachesFromMutableInputList() {
        final List<IssueSummaryDto> items = new ArrayList<>();
        items.add(new IssueSummaryDto(1L, 1L, "A", null, "opened", List.of("bug"), null, null, null, null));

        final SearchIssuesResponse response = new SearchIssuesResponse(items, 1, 1);
        final List<IssueSummaryDto> responseItems = response.items();
        items.clear();

        assertThat(responseItems).hasSize(1);
        assertThatThrownBy(() -> responseItems.add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("omits changeSets when search item has null changeSets")
    void omitsChangeSetsWhenSearchItemHasNullChangeSets() throws Exception {
        final IssueSummaryDto item = IssueSummaryDto.builder()
                .id(1L)
                .issueId(7L)
                .title("Issue")
                .description("Desc")
                .state("opened")
                .labels(List.of("bug"))
                .assignee("john.doe")
                .milestone("M1")
                .parent(null)
                .changeSets(null)
                .build();

        final String json = objectMapper.writeValueAsString(new SearchIssuesResponse(List.of(item), 1, 1));

        assertThat(objectMapper.readTree(json).path("items").get(0).has("changeSets"))
                .isFalse();
    }

    @Test
    @DisplayName("serializes change field as lowercase label")
    void serializesChangeFieldAsLowercaseLabel() throws Exception {
        final IssueSummaryDto item = searchIssueWithLabelAudit();

        final String json = objectMapper.writeValueAsString(new SearchIssuesResponse(List.of(item), 1, 1));

        assertThat(objectMapper
                        .readTree(json)
                        .path("items")
                        .get(0)
                        .path("changeSets")
                        .get(0)
                        .path("change")
                        .path("field")
                        .asText())
                .isEqualTo("label");
    }

    private IssueSummaryDto searchIssueWithLabelAudit() {
        return IssueSummaryDto.builder()
                .id(1L)
                .issueId(7L)
                .title("Issue")
                .description("Desc")
                .state("opened")
                .labels(List.of("bug"))
                .assignee("john.doe")
                .milestone("M1")
                .parent(null)
                .changeSets(List.of(LabelChangeSetDto.builder()
                        .changeType("add")
                        .changedBy(UserDto.builder()
                                .id(1L)
                                .username("root")
                                .name("Administrator")
                                .build())
                        .change(LabelChangeDto.builder()
                                .field(ChangeField.LABEL)
                                .id(73L)
                                .name("bug")
                                .build())
                        .changedAt(null)
                        .build()))
                .build();
    }
}
