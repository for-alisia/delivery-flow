package com.gitlabflow.floworchestrator.orchestration.issues.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelChangeSetTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-01-04T15:31:51.081Z");
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-03-12T09:00:00.000Z");
    private static final OffsetDateTime CHANGED_AT = OffsetDateTime.parse("2026-03-20T08:15:00.000Z");

    @Test
    @DisplayName("label change field is always label")
    void labelChangeFieldIsAlwaysLabel() {
        final LabelChange change = LabelChange.builder().id(73L).name("bug").build();

        assertThat(change.field()).isEqualTo(ChangeField.LABEL);
    }

    @Test
    @DisplayName("builders allow nullable strings for new change-set models")
    void buildersAllowNullableStringsForNewChangeSetModels() {
        final ChangedBy changedBy =
                ChangedBy.builder().id(1L).username(null).name("Administrator").build();
        final LabelChange change = LabelChange.builder().id(73L).name(null).build();
        final LabelChangeSet changeSet = LabelChangeSet.builder()
                .changeType("add")
                .changedBy(changedBy)
                .change(change)
                .changedAt(CHANGED_AT)
                .build();

        assertThat(changeSet.changedBy().username()).isNull();
        assertThat(changeSet.change().name()).isNull();
        assertThat(changeSet.changedAt()).isEqualTo(CHANGED_AT);
    }

    @Test
    @DisplayName("enriched issue detail defensively copies change sets")
    void enrichedIssueDetailDefensivelyCopiesChangeSets() {
        final LabelChangeSet changeSet = LabelChangeSet.builder()
                .changeType("add")
                .changedBy(ChangedBy.builder()
                        .id(1L)
                        .username("root")
                        .name("Administrator")
                        .build())
                .change(LabelChange.builder().id(73L).name("bug").build())
                .changedAt(CHANGED_AT)
                .build();
        final List<ChangeSet> changeSets = new ArrayList<>(List.of(changeSet));

        final EnrichedIssueDetail enriched = EnrichedIssueDetail.builder()
                .issueDetail(sampleIssueDetail())
                .changeSets(changeSets)
                .build();

        changeSets.clear();
        final List<ChangeSet> enrichedChangeSets = enriched.changeSets();
        assertThat(enriched.changeSets()).containsExactly(changeSet);
        assertThatThrownBy(() -> enrichedChangeSets.add(changeSet)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("enriched issue detail normalizes null and empty change sets")
    void enrichedIssueDetailNormalizesNullAndEmptyChangeSets() {
        final EnrichedIssueDetail withNull = EnrichedIssueDetail.builder()
                .issueDetail(sampleIssueDetail())
                .changeSets(null)
                .build();
        final EnrichedIssueDetail withEmpty = EnrichedIssueDetail.builder()
                .issueDetail(sampleIssueDetail())
                .changeSets(List.of())
                .build();

        assertThat(withNull.changeSets()).isEmpty();
        assertThat(withEmpty.changeSets()).isEmpty();
    }

    private IssueDetail sampleIssueDetail() {
        return IssueDetail.builder()
                .issueId(42L)
                .title("Fix login bug")
                .description("SSO broken")
                .state("opened")
                .labels(List.of("bug"))
                .assignees(List.of())
                .milestone(null)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .closedAt(null)
                .build();
    }
}
