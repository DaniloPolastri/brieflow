package com.briefflow.repository;

import com.briefflow.entity.ClientMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ClientMemberRepository extends JpaRepository<ClientMember, ClientMember.ClientMemberId> {
    List<ClientMember> findByClientId(Long clientId);
    List<ClientMember> findByMemberId(Long memberId);
    @Modifying
    void deleteByClientIdAndMemberId(Long clientId, Long memberId);
    @Modifying
    void deleteByClientId(Long clientId);
    boolean existsByClientIdAndMemberId(Long clientId, Long memberId);
}
