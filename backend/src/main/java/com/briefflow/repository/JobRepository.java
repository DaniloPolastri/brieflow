package com.briefflow.repository;

import com.briefflow.entity.Job;
import com.briefflow.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    @Query("""
        SELECT j FROM Job j
        LEFT JOIN FETCH j.client
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        LEFT JOIN FETCH j.createdBy
        LEFT JOIN FETCH j.files
        WHERE j.id = :id AND j.workspace.id = :workspaceId
    """)
    Optional<Job> findByIdAndWorkspaceIdWithDetails(@Param("id") Long id,
                                                    @Param("workspaceId") Long workspaceId);

    Optional<Job> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Query(value = """
        UPDATE workspaces SET job_counter = job_counter + 1
        WHERE id = :workspaceId
        RETURNING job_counter
    """, nativeQuery = true)
    @Modifying
    Long incrementAndGetJobCounter(@Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT DISTINCT j FROM Job j
        LEFT JOIN FETCH j.client c
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        WHERE j.workspace.id = :workspaceId
          AND j.archived = :archived
          AND (
               j.assignedCreative.id = :memberId
            OR j.client.id IN (
                 SELECT cm.clientId FROM ClientMember cm WHERE cm.memberId = :memberId
               )
          )
    """)
    List<Job> findVisibleToCreative(@Param("workspaceId") Long workspaceId,
                                    @Param("memberId") Long memberId,
                                    @Param("archived") Boolean archived);

    @Query("""
        SELECT j FROM Job j
        LEFT JOIN FETCH j.client
        LEFT JOIN FETCH j.assignedCreative a LEFT JOIN FETCH a.user
        WHERE j.workspace.id = :workspaceId
          AND j.archived = false
    """)
    Page<Job> findAllActiveByWorkspaceId(@Param("workspaceId") Long workspaceId, Pageable pageable);

    long countByWorkspaceIdAndStatus(Long workspaceId, JobStatus status);
}
