package com.tixie.issue.domain;

import com.tixie.issue.IssueKeyCounterRepository;
import com.tixie.project.ProjectRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.UUID;

import static jakarta.transaction.Transactional.TxType.MANDATORY;

@ApplicationScoped
public class IssueKeyGenerator {

    @Inject
    IssueKeyCounterRepository counterRepository;

    @Inject
    ProjectRepository projectRepository;

    @Transactional(MANDATORY)
    public String generate(UUID projectId) {
        var project = projectRepository.findActiveById(projectId)
                .orElseThrow(() -> new NotFoundException("Project '" + projectId + "' not found"));

        counterRepository.ensureExists(projectId);
        var counter = counterRepository.findByProjectIdForUpdate(projectId)
                .orElseThrow(() -> new IllegalStateException("Issue key counter was not initialized"));

        long newSeq = counter.lastSeq + 1;
        counter.lastSeq = newSeq;

        return project.key + "-" + newSeq;
    }
}
