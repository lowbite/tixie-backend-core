package com.tixie.project;

import com.tixie.project.api.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectModelSmokeTest {

    @Test
    void entitiesAndDtos_areUsable() {
        var p = new ProjectEntity();
        p.id = UUID.randomUUID();
        p.companyId = UUID.randomUUID();
        p.name = "Proj";
        p.key = "PROJ";
        p.createdAt = Instant.now();

        var s = new ProjectStatusEntity();
        s.id = UUID.randomUUID();
        s.projectId = p.id;
        s.name = "To Do";
        s.displayOrder = 1;
        s.isDefault = true;

        var create = new CreateProjectRequest();
        create.name = p.name;
        create.key = p.key;
        var update = new UpdateProjectRequest();
        update.name = "Renamed";
        var cstatus = new CreateProjectStatusRequest();
        cstatus.name = "Done";
        var pstatus = new PatchProjectStatusRequest();
        pstatus.name = "Done+";
        var reorder = new ReorderProjectStatusesRequest();
        reorder.statusIds = List.of(s.id);

        var resp = new ProjectResponse();
        resp.id = p.id;
        resp.statuses = List.of(new ProjectResponse.StatusRef(s.id, s.name, s.displayOrder, s.isDefault));

        var statusResp = new ProjectStatusResponse();
        statusResp.id = s.id;
        statusResp.name = s.name;
        assertEquals("To Do", resp.statuses.get(0).name());
        assertEquals("To Do", statusResp.name);
    }
}
