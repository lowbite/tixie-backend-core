# Class Coverage Checklist

Track unit-test coverage for every production class under `src/main/java/com/tixie`.

Legend:
- `[ ]` not covered yet
- `[x]` covered by at least one test

## Core
- [x] `com.tixie.TixieApp`

## Common Exceptions
- [x] `com.tixie.common.exception.ConstraintViolationExceptionMapper`
- [x] `com.tixie.common.exception.ErrorResponse`
- [x] `com.tixie.common.exception.NotFoundExceptionMapper`
- [x] `com.tixie.common.exception.ValidationException`
- [x] `com.tixie.common.exception.ValidationExceptionMapper`

## Company
- [x] `com.tixie.company.CompanyEntity`
- [x] `com.tixie.company.CompanyRepository`
- [x] `com.tixie.company.domain.CompanyService`
- [x] `com.tixie.company.api.CompanyResource`
- [x] `com.tixie.company.api.dto.CompanyResponse`
- [x] `com.tixie.company.api.dto.CreateCompanyRequest`
- [x] `com.tixie.company.api.dto.UpdateCompanyRequest`

## Project
- [x] `com.tixie.project.ProjectEntity`
- [x] `com.tixie.project.ProjectRepository`
- [x] `com.tixie.project.ProjectStatusEntity`
- [x] `com.tixie.project.ProjectStatusRepository`
- [x] `com.tixie.project.domain.ProjectStatusService`
- [x] `com.tixie.project.domain.ProjectService`
- [x] `com.tixie.project.domain.ProjectValidator`
- [x] `com.tixie.project.api.ProjectResource`
- [x] `com.tixie.project.api.ProjectStatusResource`
- [x] `com.tixie.project.api.dto.CreateProjectRequest`
- [x] `com.tixie.project.api.dto.CreateProjectStatusRequest`
- [x] `com.tixie.project.api.dto.PatchProjectStatusRequest`
- [x] `com.tixie.project.api.dto.ProjectResponse`
- [x] `com.tixie.project.api.dto.ProjectStatusResponse`
- [x] `com.tixie.project.api.dto.ReorderProjectStatusesRequest`
- [x] `com.tixie.project.api.dto.UpdateProjectRequest`

## Issue
- [x] `com.tixie.issue.IssueEntity`
- [x] `com.tixie.issue.IssueKeyCounterEntity`
- [x] `com.tixie.issue.IssueKeyCounterRepository`
- [x] `com.tixie.issue.IssueRepository`
- [x] `com.tixie.issue.domain.IssueKeyGenerator`
- [x] `com.tixie.issue.domain.IssueService`
- [x] `com.tixie.issue.domain.IssueSoftDeleteHandler`
- [x] `com.tixie.issue.domain.IssueValidator`
- [x] `com.tixie.issue.api.IssueResource`
- [x] `com.tixie.issue.api.dto.CreateIssueRequest`
- [x] `com.tixie.issue.api.dto.IssueResponse`
- [x] `com.tixie.issue.api.dto.MoveIssueRequest`
- [x] `com.tixie.issue.api.dto.PatchIssueRequest`
- [x] `com.tixie.issue.api.dto.ProjectBoardResponse`
- [x] `com.tixie.issue.api.dto.TransitionIssueRequest`
- [x] `com.tixie.issue.domain.model.IssuePriority`
- [x] `com.tixie.issue.domain.model.IssueType`
