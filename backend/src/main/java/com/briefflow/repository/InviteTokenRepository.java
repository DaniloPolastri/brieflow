package com.briefflow.repository;

import com.briefflow.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {
    Optional<InviteToken> findByToken(String token);

    @Query("SELECT t FROM InviteToken t JOIN FETCH t.workspace JOIN FETCH t.invitedBy WHERE t.token = :token")
    Optional<InviteToken> findByTokenWithDetails(String token);

    List<InviteToken> findByWorkspaceIdAndUsedFalse(Long workspaceId);
    List<InviteToken> findByWorkspaceIdAndEmailAndUsedFalse(Long workspaceId, String email);
}
