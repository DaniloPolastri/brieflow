package com.briefflow.repository;

import com.briefflow.entity.Member;
import com.briefflow.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByWorkspaceId(Long workspaceId);

    @Query("SELECT m FROM Member m JOIN FETCH m.user WHERE m.workspace.id = :workspaceId")
    List<Member> findByWorkspaceIdWithUser(Long workspaceId);
    Optional<Member> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    Optional<Member> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    @Query("SELECT m FROM Member m WHERE m.user.id = :userId ORDER BY m.createdAt ASC")
    Optional<Member> findFirstByUserId(Long userId);

    long countByWorkspaceIdAndRole(Long workspaceId, MemberRole role);
}
