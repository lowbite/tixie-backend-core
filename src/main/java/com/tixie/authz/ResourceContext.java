package com.tixie.authz;

import java.util.UUID;

public record ResourceContext(ResourceType type, UUID resourceId, UUID companyId, UUID projectId) {
}
