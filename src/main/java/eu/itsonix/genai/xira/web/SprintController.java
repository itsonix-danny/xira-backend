package eu.itsonix.genai.xira.web;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import eu.itsonix.genai.xira.service.SprintService;
import eu.itsonix.genai.xira.web.api.SprintsApi;
import eu.itsonix.genai.xira.web.model.AddSprintRequest;
import eu.itsonix.genai.xira.web.model.SprintResponse;
import eu.itsonix.genai.xira.web.model.UpdateSprintRequest;

@RestController
@RequiredArgsConstructor
public class SprintController implements SprintsApi {

    private final SprintService sprintService;

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> createSprint(final String key, final AddSprintRequest addSprintRequest) {
        final String sprintId = sprintService.createSprint(key, addSprintRequest);
        return ResponseEntity
                .created(URI.create(String.format("/projects/%s/sprints/%s", key, sprintId))).build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<SprintResponse> updateSprint(final String key, final UUID sprintId,
            final UpdateSprintRequest updateSprintRequest) {
        final SprintResponse sprint = sprintService.updateSprint(key, sprintId.toString(), updateSprintRequest);
        return ResponseEntity.ok(sprint);
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<Void> deleteSprint(final String key, final UUID sprintId) {
        sprintService.deleteSprint(key, sprintId.toString());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<SprintResponse> startSprint(final String key, final UUID sprintId) {
        final SprintResponse sprint = sprintService.startSprint(key, sprintId.toString());
        return ResponseEntity.ok(sprint);
    }

    @Override
    @PreAuthorize("@authService.isProjectMember(#key)")
    public ResponseEntity<SprintResponse> finishSprint(final String key, final UUID sprintId) {
        final SprintResponse sprint = sprintService.finishSprint(key, sprintId.toString());
        return ResponseEntity.ok(sprint);
    }
}
