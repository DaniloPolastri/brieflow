package com.briefflow.repository;

import com.briefflow.entity.ClientMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientMemberRepository extends JpaRepository<ClientMember, ClientMember.ClientMemberId> {
    List<ClientMember> findByClientId(Long clientId);
    List<ClientMember> findByMemberId(Long memberId);
    void deleteByClientIdAndMemberId(Long clientId, Long memberId);
    boolean existsByClientIdAndMemberId(Long clientId, Long memberId);
}
