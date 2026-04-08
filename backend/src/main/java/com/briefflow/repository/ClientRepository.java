package com.briefflow.repository;

import com.briefflow.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    List<Client> findByWorkspaceIdAndActiveOrderByNameAsc(Long workspaceId, Boolean active);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    List<Client> searchByNameOrCompany(@Param("workspaceId") Long workspaceId, @Param("search") String search);

    @Query("SELECT c FROM Client c WHERE c.workspace.id = :workspaceId " +
           "AND c.active = :active " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC")
    List<Client> searchByNameOrCompanyAndActive(@Param("workspaceId") Long workspaceId, @Param("search") String search, @Param("active") Boolean active);

    Optional<Client> findByIdAndWorkspaceId(Long id, Long workspaceId);

    @Query(value = "SELECT c.* FROM clients c INNER JOIN client_members cm ON c.id = cm.client_id " +
           "WHERE cm.member_id = :memberId AND c.workspace_id = :workspaceId ORDER BY c.name ASC", nativeQuery = true)
    List<Client> findByMemberIdAndWorkspaceId(@Param("memberId") Long memberId, @Param("workspaceId") Long workspaceId);

    @Query(value = "SELECT c.* FROM clients c INNER JOIN client_members cm ON c.id = cm.client_id " +
           "WHERE cm.member_id = :memberId AND c.workspace_id = :workspaceId AND c.active = :active ORDER BY c.name ASC", nativeQuery = true)
    List<Client> findByMemberIdAndWorkspaceIdAndActive(@Param("memberId") Long memberId, @Param("workspaceId") Long workspaceId, @Param("active") Boolean active);

    @Query(value = "SELECT c.* FROM clients c INNER JOIN client_members cm ON c.id = cm.client_id " +
           "WHERE cm.member_id = :memberId AND c.workspace_id = :workspaceId " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC", nativeQuery = true)
    List<Client> searchByMemberIdAndNameOrCompany(@Param("memberId") Long memberId, @Param("workspaceId") Long workspaceId, @Param("search") String search);

    @Query(value = "SELECT c.* FROM clients c INNER JOIN client_members cm ON c.id = cm.client_id " +
           "WHERE cm.member_id = :memberId AND c.workspace_id = :workspaceId AND c.active = :active " +
           "AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.name ASC", nativeQuery = true)
    List<Client> searchByMemberIdAndNameOrCompanyAndActive(@Param("memberId") Long memberId, @Param("workspaceId") Long workspaceId, @Param("search") String search, @Param("active") Boolean active);
}
