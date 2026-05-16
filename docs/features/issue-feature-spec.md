# Feature Spec: Issue Entity

**Stack:** Java 17 + Quarkus + PostgreSQL + Hibernate Panache + Liquibase + OpenAPI  
**Convention:** Panache active record pattern, `@Transactional` on service layer.

---

## 1. Database Schema

Migrations use **Liquibase XML** changelogs. Master changelog includes all changesets in order.

### db/changelog/db.changelog-master.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                       http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/001-create-companies.xml"/>
    <include file="db/changelog/002-create-projects.xml"/>
    <include file="db/changelog/003-create-project-statuses.xml"/>
    <include file="db/changelog/004-create-issue-key-counters.xml"/>
    <include file="db/changelog/005-create-issues.xml"/>

</databaseChangeLog>
```

### 001-create-companies.xml
```xml
<databaseChangeLog ...>
    <changeSet id="001-create-companies" author="dev">
        <createTable tableName="companies">
            <column name="id"         type="UUID"         defaultValueComputed="gen_random_uuid()"><constraints primaryKey="true"/></column>
            <column name="name"       type="VARCHAR(255)"><constraints nullable="false"/></column>
            <column name="created_at" type="TIMESTAMP"    defaultValueComputed="now()"><constraints nullable="false"/></column>
            <column name="deleted_at" type="TIMESTAMP"/>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

### 002-create-projects.xml
```xml
<changeSet id="002-create-projects" author="dev">
    <createTable tableName="projects">
        <column name="id"         type="UUID"        defaultValueComputed="gen_random_uuid()"><constraints primaryKey="true"/></column>
        <column name="company_id" type="UUID"><constraints nullable="false" foreignKeyName="fk_projects_company" references="companies(id)"/></column>
        <column name="name"       type="VARCHAR(255)"><constraints nullable="false"/></column>
        <column name="key"        type="VARCHAR(10)"><constraints nullable="false" unique="true"/></column>
        <column name="created_at" type="TIMESTAMP"   defaultValueComputed="now()"><constraints nullable="false"/></column>
        <column name="deleted_at" type="TIMESTAMP"/>
    </createTable>
    <createIndex tableName="projects" indexName="idx_projects_company_id">
        <column name="company_id"/>
    </createIndex>
</changeSet>
```

### 003-create-project-statuses.xml
```xml
<changeSet id="003-create-project-statuses" author="dev">
    <createTable tableName="project_statuses">
        <column name="id"            type="UUID"         defaultValueComputed="gen_random_uuid()"><constraints primaryKey="true"/></column>
        <column name="project_id"    type="UUID"><constraints nullable="false" foreignKeyName="fk_statuses_project" references="projects(id)"/></column>
        <column name="name"          type="VARCHAR(100)"><constraints nullable="false"/></column>
        <column name="display_order" type="INT"          defaultValueNumeric="0"><constraints nullable="false"/></column>
        <column name="is_default"    type="BOOLEAN"      defaultValueBoolean="false"><constraints nullable="false"/></column>
    </createTable>
    <addUniqueConstraint tableName="project_statuses" columnNames="project_id, name"
                         constraintName="uq_project_statuses_project_name"/>
</changeSet>
```

### 004-create-issue-key-counters.xml
```xml
<!-- One row per project. Incremented with SELECT ... FOR UPDATE to avoid race conditions. -->
<changeSet id="004-create-issue-key-counters" author="dev">
    <createTable tableName="issue_key_counters">
        <column name="project_id" type="UUID"><constraints primaryKey="true" foreignKeyName="fk_counters_project" references="projects(id)"/></column>
        <column name="last_seq"   type="BIGINT" defaultValueNumeric="0"><constraints nullable="false"/></column>
    </createTable>
</changeSet>
```

### 005-create-issues.xml
```xml
<changeSet id="005-create-issues" author="dev">
    <createTable tableName="issues">
        <column name="id"          type="UUID"        defaultValueComputed="gen_random_uuid()"><constraints primaryKey="true"/></column>
        <column name="issue_key"   type="VARCHAR(20)"><constraints nullable="false" unique="true"/></column>
        <column name="title"       type="VARCHAR(255)"><constraints nullable="false"/></column>
        <column name="description" type="TEXT"/>
        <column name="type"        type="VARCHAR(20)"><constraints nullable="false"/></column>
        <column name="priority"    type="VARCHAR(20)" defaultValue="MEDIUM"><constraints nullable="false"/></column>
        <column name="status_id"   type="UUID"><constraints nullable="false" foreignKeyName="fk_issues_status"  references="project_statuses(id)"/></column>
        <column name="project_id"  type="UUID"><constraints nullable="false" foreignKeyName="fk_issues_project" references="projects(id)"/></column>
        <column name="parent_id"   type="UUID"><constraints foreignKeyName="fk_issues_parent" references="issues(id)"/></column>
        <column name="created_at"  type="TIMESTAMP"   defaultValueComputed="now()"><constraints nullable="false"/></column>
        <column name="updated_at"  type="TIMESTAMP"   defaultValueComputed="now()"><constraints nullable="false"/></column>
        <column name="deleted_at"  type="TIMESTAMP"/>
    </createTable>
    <createIndex tableName="issues" indexName="idx_issues_project_id"><column name="project_id"/></createIndex>
    <createIndex tableName="issues" indexName="idx_issues_parent_id"><column name="parent_id"/></createIndex>
    <createIndex tableName="issues" indexName="idx_issues_status_id"><column name="status_id"/></createIndex>
</changeSet>
```

---

## 2. Project Structure

```
src/main/java/com/yourapp/
├── company/
│   ├── CompanyEntity.java
│   └── CompanyRepository.java
├── project/
│   ├── ProjectEntity.java
│   ├── ProjectRepository.java
│   ├── ProjectStatusEntity.java
│   └── ProjectStatusRepository.java
└── issue/
    ├── api/
    │   ├── IssueResource.java
    │   └── dto/
    │       ├── CreateIssueRequest.java
    │       ├── PatchIssueRequest.java
    │       └── IssueResponse.java
    ├── domain/
    │   ├── IssueService.java
    │   ├── IssueValidator.java
    │   ├── IssueKeyGenerator.java
    │   ├── IssueSoftDeleteHandler.java
    │   └── model/
    │       ├── IssueType.java
    │       └── IssuePriority.java
    ├── IssueEntity.java
    ├── IssueRepository.java
    ├── IssueKeyCounterEntity.java
    └── IssueKeyCounterRepository.java

src/main/resources/db/changelog/
├── db.changelog-master.xml
├── 001-create-companies.xml
├── 002-create-projects.xml
├── 003-create-project-statuses.xml
├── 004-create-issue-key-counters.xml
└── 005-create-issues.xml
```

---

## 3. Domain Rules

### IssueType hierarchy
```
EPIC → STORY → TASK → SUBTASK
```
Each type has exactly one allowed parent type. `EPIC` cannot have a parent.

```java
public enum IssueType {
    EPIC, STORY, TASK, SUBTASK;

    public Optional<IssueType> allowedParentType() {
        return switch (this) {
            case EPIC    -> Optional.empty();
            case STORY   -> Optional.of(EPIC);
            case TASK    -> Optional.of(STORY);
            case SUBTASK -> Optional.of(TASK);
        };
    }
}
```

### Validation rules — CREATE
1. `projectId` exists and `deleted_at IS NULL`
2. Project's `company_id` → company exists and `deleted_at IS NULL`
3. `statusId` belongs to this `projectId`
4. If `parentId` provided: parent exists, not deleted, same `projectId`, and `parent.type == issue.type.allowedParentType()`
5. `EPIC` must have no `parentId`

### Validation rules — PATCH
- `type` is **immutable** — reject if provided
- Re-validate `statusId` and `parentId` using the same rules as CREATE if they are present in the request

### Issue Key Generation
Must run **inside the same transaction** as the issue insert:
```
1. SELECT * FROM issue_key_counters WHERE project_id = ? FOR UPDATE
2. If no row → INSERT (project_id, last_seq = 0)
3. new_seq = last_seq + 1
4. UPDATE issue_key_counters SET last_seq = new_seq WHERE project_id = ?
5. return project.key + "-" + new_seq   -- e.g. "PROJ-1"
```

### Soft Delete Cascade
Use a single recursive CTE — no application-level loops:
```sql
WITH RECURSIVE descendants AS (
    SELECT id FROM issues WHERE id = :rootId
    UNION ALL
    SELECT i.id FROM issues i
    INNER JOIN descendants d ON i.parent_id = d.id
    WHERE i.deleted_at IS NULL
)
UPDATE issues
SET deleted_at = now(), updated_at = now()
WHERE id IN (SELECT id FROM descendants);
```

---

## 4. REST API

Base path: `/projects/{projectId}/issues`

| Method   | Path          | Body                 | Success | Error codes   |
|----------|---------------|----------------------|---------|---------------|
| `POST`   | `/`           | `CreateIssueRequest` | `201`   | `400`, `404`  |
| `GET`    | `/`           | —                    | `200`   | `404`         |
| `GET`    | `/{issueId}`  | —                    | `200`   | `404`         |
| `PATCH`  | `/{issueId}`  | `PatchIssueRequest`  | `200`   | `400`, `404`  |
| `DELETE` | `/{issueId}`  | —                    | `204`   | `404`         |

### CreateIssueRequest
| Field         | Type            | Required | Notes                  |
|---------------|-----------------|----------|------------------------|
| `title`       | `String`        | yes      | max 255 chars          |
| `description` | `String`        | no       |                        |
| `type`        | `IssueType`     | yes      |                        |
| `priority`    | `IssuePriority` | no       | default `MEDIUM`       |
| `statusId`    | `UUID`          | yes      | must belong to project |
| `parentId`    | `UUID`          | no       | hierarchy rules apply  |

### PatchIssueRequest
All fields optional. Only non-null fields are applied. `type` must not be present.

| Field         | Type            |
|---------------|-----------------|
| `title`       | `String`        |
| `description` | `String`        |
| `priority`    | `IssuePriority` |
| `statusId`    | `UUID`          |
| `parentId`    | `UUID`          |

### IssueResponse
| Field         | Type            |
|---------------|-----------------|
| `id`          | `UUID`          |
| `issueKey`    | `String`        |
| `title`       | `String`        |
| `description` | `String`        |
| `type`        | `IssueType`     |
| `priority`    | `IssuePriority` |
| `status`      | `{ id, name }`  |
| `projectId`   | `UUID`          |
| `parentId`    | `UUID`          |
| `createdAt`   | `Instant`       |
| `updatedAt`   | `Instant`       |

### Error Response (all errors)
```json
{ "code": "ISSUE_NOT_FOUND", "message": "Issue '...' not found" }
```

---

## 5. Atomic Tasks

### TASK-1 — Liquibase migrations
Add `quarkus-liquibase` dependency. Create `db.changelog-master.xml` and 5 changeset files from Section 1.  
Configure `quarkus.liquibase.migrate-at-start=true` in `application.properties`.  
**Done when:** `./mvnw quarkus:dev` starts without Liquibase errors and all tables exist in DB.

---

### TASK-2 — Company & Project persistence layer
Entities and repositories for `companies`, `projects`, `project_statuses`.  
Repositories expose `findActiveById` (filters `deleted_at IS NULL`) and status lookup by `projectId`.  
**Done when:** Repositories compile and queries work against DB.

---

### TASK-3 — IssueType & IssuePriority enums
Implement enums with `allowedParentType()` as shown in Section 3.  
**Done when:** `IssueType.SUBTASK.allowedParentType()` returns `Optional.of(TASK)`.

---

### TASK-4 — Issue persistence layer
`IssueEntity`, `IssueKeyCounterEntity` and their repositories.  
`IssueKeyCounterRepository` must use a native `SELECT ... FOR UPDATE` query.  
**Done when:** Entities map correctly to DB schema, pessimistic lock query executes.

---

### TASK-5 — IssueKeyGenerator
Service that increments `issue_key_counters` within the caller's transaction and returns `"KEY-N"`.  
Must not open its own transaction — annotate with `@Transactional(MANDATORY)`.  
**Done when:** Two concurrent requests for the same project produce unique, sequential keys.

---

### TASK-6 — IssueValidator
Stateless service with `validateCreate(CreateIssueRequest, UUID projectId)` and `validatePatch(PatchIssueRequest, IssueEntity existing)`.  
Throws `NotFoundException` (404) or `ValidationException` (400) per Section 3 rules.  
**Done when:** Unit tests cover all valid and invalid hierarchy combinations.

---

### TASK-7 — IssueSoftDeleteHandler
Single `@Transactional` method that executes the recursive CTE from Section 3 via a native query.  
**Done when:** Deleting a STORY cascades to all TASK and SUBTASK descendants in one DB round-trip.

---

### TASK-8 — IssueService
Orchestrates CRUD using `IssueValidator`, `IssueKeyGenerator`, `IssueSoftDeleteHandler` and repositories.  
All write methods are `@Transactional`.  
**Done when:** End-to-end integration test passes for all 5 operations.

---

### TASK-9 — DTOs & IssueResource
`CreateIssueRequest`, `PatchIssueRequest`, `IssueResponse` with Bean Validation annotations.  
`IssueResource` maps all endpoints from Section 4 with full OpenAPI (`@Operation`, `@APIResponse`, `@Tag`).  
**Done when:** Swagger UI at `/q/swagger-ui` shows all 5 endpoints with correct request/response schemas.

---

### TASK-10 — Global Exception Mapper
`@Provider` that maps `NotFoundException` → 404, `ValidationException` → 400, `ConstraintViolationException` → 400.  
All responses use the `{ "code", "message" }` structure from Section 4.  
**Done when:** Unknown `projectId` returns `404` with JSON body, not an HTML error page.

---

## Execution Order

```
TASK-1
  └── TASK-2
  └── TASK-3 ──┐
  └── TASK-4 ──┤
               ├── TASK-5 ──┐
               ├── TASK-6 ──┼── TASK-8 ── TASK-9
               └── TASK-7 ──┘
TASK-10  (independent, any time)
```
