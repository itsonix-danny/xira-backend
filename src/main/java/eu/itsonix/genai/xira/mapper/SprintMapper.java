package eu.itsonix.genai.xira.mapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import eu.itsonix.genai.xira.jpa.entity.Sprint;
import eu.itsonix.genai.xira.web.model.SprintResponse;
import eu.itsonix.genai.xira.web.model.SprintState;

public final class SprintMapper {

    private SprintMapper() {
    }

    public static SprintResponse toSprintResponse(final Sprint sprint) {
        return new SprintResponse().id(UUID.fromString(sprint.getId()))
                .name(sprint.getName())
                .goal(sprint.getGoal())
                .state(toSprintState(sprint.getState()))
                .startedAt(toOffsetDateTime(sprint.getStartedAt()))
                .finishedAt(toOffsetDateTime(sprint.getFinishedAt()));
    }

    private static SprintState toSprintState(final eu.itsonix.genai.xira.jpa.entity.SprintState state) {
        if (state == null) {
            return null;
        }
        return SprintState.fromValue(state.name());
    }

    private static OffsetDateTime toOffsetDateTime(final Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
