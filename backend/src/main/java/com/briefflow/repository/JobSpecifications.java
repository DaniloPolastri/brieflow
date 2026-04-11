package com.briefflow.repository;

import com.briefflow.entity.Job;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> inWorkspace(Long workspaceId) {
        return (root, q, cb) -> cb.equal(root.get("workspace").get("id"), workspaceId);
    }

    public static Specification<Job> notArchived() {
        return (root, q, cb) -> cb.equal(root.get("archived"), false);
    }

    public static Specification<Job> hasStatus(JobStatus status) {
        return (root, q, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Job> hasType(JobType type) {
        return (root, q, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("type"), type);
    }

    public static Specification<Job> hasPriority(JobPriority priority) {
        return (root, q, cb) -> priority == null ? cb.conjunction() : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Job> forClient(Long clientId) {
        return (root, q, cb) -> clientId == null ? cb.conjunction() : cb.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Job> assignedCreative(Long memberId) {
        return (root, q, cb) -> memberId == null ? cb.conjunction() : cb.equal(root.get("assignedCreative").get("id"), memberId);
    }
}
