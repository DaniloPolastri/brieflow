package com.briefflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "client_members")
@IdClass(ClientMember.ClientMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class ClientMember {

    @Id
    @Column(name = "client_id")
    private Long clientId;

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ClientMemberId implements Serializable {
        private Long clientId;
        private Long memberId;

        public ClientMemberId(Long clientId, Long memberId) {
            this.clientId = clientId;
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientMemberId that = (ClientMemberId) o;
            return Objects.equals(clientId, that.clientId) && Objects.equals(memberId, that.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clientId, memberId);
        }
    }
}
