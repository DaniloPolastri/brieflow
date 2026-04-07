# RF02 — Workspace e Membros Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement multi-tenancy via workspaces with automatic creation on register, member invitation by copyable link, role-based permissions (OWNER/MANAGER/CREATIVE), and functional positions (10 fixed roles).

**Architecture:** Backend adds Workspace, Member, InviteToken entities with Flyway migrations. JwtService gains workspaceId claim for multi-tenant isolation. MemberService handles invite/accept/remove flows. Frontend adds member list page, invite dialog, accept-invite page, settings page, and modifies register to include workspace name.

**Tech Stack:** Java 21, Spring Boot 3.5, jjwt, Flyway, JUnit 5, Mockito, Testcontainers | Angular 21, PrimeNG 21, Tailwind CSS v4, Vitest

---

## Task Summary

### Backend
- [x] Task B1: Flyway migrations V3, V4, V5 ⚡ PARALLEL GROUP A
- [x] Task B2: Enums MemberRole + MemberPosition ⚡ PARALLEL GROUP A
- [x] Task B3: Entities Workspace + Member + InviteToken + unit tests ⚡ PARALLEL GROUP A
- [x] Task B4: Repositories WorkspaceRepository + MemberRepository + InviteTokenRepository (depende de B1, B2, B3)
- [x] Task B5: DTOs workspace/member/invoke + modify RegisterRequestDTO + UserInfoDTO ⚡ PARALLEL GROUP A
- [x] Task B6: JwtService + JwtFilter modifications (workspaceId claim) + unit tests (depende de B2)
- [x] Task B7: Services WorkspaceService + MemberService + modify AuthServiceImpl + unit tests (depende de B3, B4, B5, B6)
- [x] Task B8: Controllers WorkspaceController + MemberController + InviteController + SecurityConfig + integration tests (depende de B7)

### Frontend
- [x] Task F1: Models member.model.ts + workspace.model.ts + invite.model.ts + modify user.model.ts ⚡ PARALLEL GROUP A
- [x] Task F2: Services member-api + workspace-api + invite-api (depende de F1)
- [x] Task F3: Auth interceptor modification + role guard ⚡ PARALLEL GROUP A
- [x] Task F4: Register page modification — add workspaceName field (depende de F1, B8 backend)
- [x] Task F5: Invite member dialog component + tests (depende de F2)
- [x] Task F6: Member list page + tests (depende de F2, F5, B8 backend)
- [x] Task F7: Accept invite page + tests (depende de F2, F3, B8 backend)
- [x] Task F8: Settings page + tests (depende de F2, B8 backend)
- [x] Task F9: Routes — members.routes.ts + settings.routes.ts + modify auth.routes.ts + app.routes.ts (depende de F4-F8)

---

## Task B1: Flyway Migrations (V3, V4, V5)

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__create_workspaces_table.sql`
- Create: `backend/src/main/resources/db/migration/V4__create_members_table.sql`
- Create: `backend/src/main/resources/db/migration/V5__create_invite_tokens_table.sql`

- [ ] **Step 1: Create V3 migration**

```sql
CREATE TABLE workspaces (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workspaces_slug ON workspaces(slug);
```

- [ ] **Step 2: Create V4 migration**

```sql
CREATE TABLE members (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    position        VARCHAR(30) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_members_user_workspace UNIQUE (user_id, workspace_id)
);

CREATE INDEX idx_members_workspace_id ON members(workspace_id);
CREATE INDEX idx_members_user_id ON members(user_id);
```

- [ ] **Step 3: Create V5 migration**

```sql
CREATE TABLE invite_tokens (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    position        VARCHAR(30) NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    invited_by      BIGINT NOT NULL REFERENCES users(id),
    expires_at      TIMESTAMP NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invite_tokens_token ON invite_tokens(token);
CREATE INDEX idx_invite_tokens_workspace_id ON invite_tokens(workspace_id);
```

- [ ] **Step 4: Verify build compiles**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration/V3__create_workspaces_table.sql backend/src/main/resources/db/migration/V4__create_members_table.sql backend/src/main/resources/db/migration/V5__create_invite_tokens_table.sql
git commit -m "feat: add Flyway migrations for workspaces, members, invite_tokens"
```

---

## Task B2: Enums MemberRole + MemberPosition

**Files:**
- Create: `backend/src/main/java/com/briefflow/enums/MemberRole.java`
- Create: `backend/src/main/java/com/briefflow/enums/MemberPosition.java`

- [ ] **Step 1: Create MemberRole enum**

```java
package com.briefflow.enums;

public enum MemberRole {
    OWNER,
    MANAGER,
    CREATIVE
}
```

- [ ] **Step 2: Create MemberPosition enum**

```java
package com.briefflow.enums;

public enum MemberPosition {
    DESIGNER_GRAFICO,
    EDITOR_DE_VIDEO,
    SOCIAL_MEDIA,
    COPYWRITER,
    GESTOR_DE_TRAFEGO,
    DIRETOR_DE_ARTE,
    ATENDIMENTO,
    FOTOGRAFO,
    ILUSTRADOR,
    MOTION_DESIGNER
}
```

- [ ] **Step 3: Verify build compiles**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/briefflow/enums/
git commit -m "feat: add MemberRole and MemberPosition enums"
```

---

## Task B3: Entities Workspace + Member + InviteToken + unit tests

**Files:**
- Create: `backend/src/main/java/com/briefflow/entity/Workspace.java`
- Create: `backend/src/main/java/com/briefflow/entity/Member.java`
- Create: `backend/src/main/java/com/briefflow/entity/InviteToken.java`
- Create: `backend/src/test/java/com/briefflow/unit/entity/WorkspaceTest.java`
- Create: `backend/src/test/java/com/briefflow/unit/entity/InviteTokenTest.java`

- [ ] **Step 1: Write Workspace test**

```java
package com.briefflow.unit.entity;

import com.briefflow.entity.Workspace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceTest {

    @Test
    void should_generateSlug_when_prePersist() {
        Workspace workspace = new Workspace();
        workspace.setName("Agencia Criativa Digital");
        workspace.onCreate();

        assertEquals("agencia-criativa-digital", workspace.getSlug());
        assertNotNull(workspace.getCreatedAt());
        assertNotNull(workspace.getUpdatedAt());
    }

    @Test
    void should_handleSpecialChars_when_generatingSlug() {
        Workspace workspace = new Workspace();
        workspace.setName("Café & Design Ltda.");
        workspace.onCreate();

        assertEquals("caf-design-ltda", workspace.getSlug());
    }
}
```

- [ ] **Step 2: Write InviteToken test**

```java
package com.briefflow.unit.entity;

import com.briefflow.entity.InviteToken;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class InviteTokenTest {

    @Test
    void should_returnTrue_when_tokenIsExpired() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        token.setUsed(false);

        assertTrue(token.isExpired());
        assertFalse(token.isUsable());
    }

    @Test
    void should_returnFalse_when_tokenIsNotExpired() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(false);

        assertFalse(token.isExpired());
        assertTrue(token.isUsable());
    }

    @Test
    void should_returnFalse_when_tokenIsUsed() {
        InviteToken token = new InviteToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(true);

        assertFalse(token.isUsable());
    }
}
```

- [ ] **Step 3: Implement Workspace entity**

```java
package com.briefflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slug == null && name != null) {
            slug = generateSlug(name);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
```

- [ ] **Step 4: Implement Member entity**

```java
package com.briefflow.entity;

import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberPosition position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 5: Implement InviteToken entity**

```java
package com.briefflow.entity;

import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "invite_tokens")
@Getter
@Setter
@NoArgsConstructor
public class InviteToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberPosition position;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }
}
```

- [ ] **Step 6: Run tests**

Run: `cd backend && ./mvnw test -Dtest="com.briefflow.unit.entity.WorkspaceTest,com.briefflow.unit.entity.InviteTokenTest" -pl .`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/briefflow/entity/Workspace.java backend/src/main/java/com/briefflow/entity/Member.java backend/src/main/java/com/briefflow/entity/InviteToken.java backend/src/test/java/com/briefflow/unit/entity/WorkspaceTest.java backend/src/test/java/com/briefflow/unit/entity/InviteTokenTest.java
git commit -m "feat: add Workspace, Member, InviteToken entities with tests"
```

---

## Task B4: Repositories

**Files:**
- Create: `backend/src/main/java/com/briefflow/repository/WorkspaceRepository.java`
- Create: `backend/src/main/java/com/briefflow/repository/MemberRepository.java`
- Create: `backend/src/main/java/com/briefflow/repository/InviteTokenRepository.java`

- [ ] **Step 1: Create WorkspaceRepository**

```java
package com.briefflow.repository;

import com.briefflow.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    Optional<Workspace> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
```

- [ ] **Step 2: Create MemberRepository**

```java
package com.briefflow.repository;

import com.briefflow.entity.Member;
import com.briefflow.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByWorkspaceId(Long workspaceId);
    Optional<Member> findByUserIdAndWorkspaceId(Long userId, Long workspaceId);
    Optional<Member> findByIdAndWorkspaceId(Long id, Long workspaceId);
    boolean existsByUserIdAndWorkspaceId(Long userId, Long workspaceId);

    @Query("SELECT m FROM Member m WHERE m.user.id = :userId")
    Optional<Member> findFirstByUserId(Long userId);

    long countByWorkspaceIdAndRole(Long workspaceId, MemberRole role);
}
```

- [ ] **Step 3: Create InviteTokenRepository**

```java
package com.briefflow.repository;

import com.briefflow.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, Long> {
    Optional<InviteToken> findByToken(String token);
    List<InviteToken> findByWorkspaceIdAndUsedFalse(Long workspaceId);
}
```

- [ ] **Step 4: Verify build compiles**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/briefflow/repository/WorkspaceRepository.java backend/src/main/java/com/briefflow/repository/MemberRepository.java backend/src/main/java/com/briefflow/repository/InviteTokenRepository.java
git commit -m "feat: add WorkspaceRepository, MemberRepository, InviteTokenRepository"
```

---

## Task B5: DTOs

**Files:**
- Create: `backend/src/main/java/com/briefflow/dto/workspace/WorkspaceResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/workspace/UpdateWorkspaceRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/MemberResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/InviteMemberRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/UpdateMemberRoleRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/InviteTokenResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/InviteInfoResponseDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/AcceptInviteRequestDTO.java`
- Create: `backend/src/main/java/com/briefflow/dto/member/MembersListResponseDTO.java`
- Modify: `backend/src/main/java/com/briefflow/dto/auth/RegisterRequestDTO.java`
- Modify: `backend/src/main/java/com/briefflow/dto/auth/UserInfoDTO.java`

All DTO code is specified in the backend architect's deliverable (Section 5). Each is a Java `record` with Bean Validation annotations where applicable.

- [ ] **Step 1: Create all workspace DTOs** (WorkspaceResponseDTO, UpdateWorkspaceRequestDTO)
- [ ] **Step 2: Create all member DTOs** (MemberResponseDTO, InviteMemberRequestDTO, UpdateMemberRoleRequestDTO, InviteTokenResponseDTO, InviteInfoResponseDTO, AcceptInviteRequestDTO, MembersListResponseDTO)
- [ ] **Step 3: Modify RegisterRequestDTO** — add `workspaceName` field with `@NotBlank` + `@Size(max=150)`
- [ ] **Step 4: Modify UserInfoDTO** — add `workspaceId`, `workspaceName`, `role`, `position` fields
- [ ] **Step 5: Verify build compiles**

Run: `cd backend && ./mvnw compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/briefflow/dto/
git commit -m "feat: add workspace/member/invite DTOs, modify RegisterRequestDTO and UserInfoDTO"
```

---

## Task B6: JwtService + JwtFilter modifications + unit tests

**Files:**
- Modify: `backend/src/main/java/com/briefflow/security/JwtService.java`
- Modify: `backend/src/main/java/com/briefflow/security/JwtFilter.java`
- Modify: `backend/src/test/java/com/briefflow/unit/security/JwtServiceTest.java`
- Modify: `backend/src/test/java/com/briefflow/unit/security/JwtFilterTest.java`

- [ ] **Step 1: Update JwtService** — change `generateAccessToken` to accept `workspaceId`, add it as JWT claim, add `extractWorkspaceId` method

Key changes:
```java
public String generateAccessToken(Long userId, String email, Long workspaceId) {
    return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("workspaceId", workspaceId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
            .signWith(key)
            .compact();
}

public Long extractWorkspaceId(String token) {
    return parseClaims(token).get("workspaceId", Long.class);
}
```

- [ ] **Step 2: Update JwtFilter** — extract `userId` and `workspaceId` from token and set as request attributes

Key changes (after setting auth):
```java
request.setAttribute("userId", userId);
request.setAttribute("workspaceId", workspaceId);
```

- [ ] **Step 3: Update JwtServiceTest** — add test for workspaceId claim

```java
@Test
void should_includeWorkspaceId_when_generatingToken() {
    String token = jwtService.generateAccessToken(1L, "user@test.com", 10L);
    assertEquals(10L, jwtService.extractWorkspaceId(token));
}
```

- [ ] **Step 4: Update JwtFilterTest** — add test for workspaceId attribute

Full test code in backend architect deliverable Section 9 (B6 tests).

- [ ] **Step 5: Run tests**

Run: `cd backend && ./mvnw test -Dtest="com.briefflow.unit.security.*" -pl .`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/briefflow/security/ backend/src/test/java/com/briefflow/unit/security/
git commit -m "feat: add workspaceId claim to JWT, set userId/workspaceId as request attributes"
```

---

## Task B7: Services + unit tests

**Files:**
- Create: `backend/src/main/java/com/briefflow/service/WorkspaceService.java`
- Create: `backend/src/main/java/com/briefflow/service/impl/WorkspaceServiceImpl.java`
- Create: `backend/src/main/java/com/briefflow/service/MemberService.java`
- Create: `backend/src/main/java/com/briefflow/service/impl/MemberServiceImpl.java`
- Modify: `backend/src/main/java/com/briefflow/service/impl/AuthServiceImpl.java`
- Create: `backend/src/test/java/com/briefflow/unit/service/WorkspaceServiceImplTest.java`
- Create: `backend/src/test/java/com/briefflow/unit/service/MemberServiceImplTest.java`
- Modify: `backend/src/test/java/com/briefflow/unit/service/AuthServiceImplTest.java`

All service code and complete test code is in the backend architect deliverable (Sections 6 and 9 — B7 tests).

Key changes to AuthServiceImpl:
- Add `WorkspaceRepository` and `MemberRepository` dependencies
- `register()` now creates Workspace + Member(OWNER, DIRETOR_DE_ARTE)
- `generateTokenResponse` overloaded: one version takes `(User, Member)`, other takes `(User)` and looks up member
- `login()` and `refresh()` now include workspace info in JWT and response

- [ ] **Step 1: Write WorkspaceServiceImplTest** (4 tests)
- [ ] **Step 2: Implement WorkspaceService interface + WorkspaceServiceImpl**
- [ ] **Step 3: Run WorkspaceServiceImpl tests** — Expected: PASS
- [ ] **Step 4: Write MemberServiceImplTest** (13 tests covering invite, remove, updateRole, getInviteInfo, acceptInvite)
- [ ] **Step 5: Implement MemberService interface + MemberServiceImpl**
- [ ] **Step 6: Run MemberServiceImpl tests** — Expected: PASS
- [ ] **Step 7: Update AuthServiceImpl** — add workspace/member creation in register, update login/refresh to include workspace info
- [ ] **Step 8: Update AuthServiceImplTest** — test that register creates workspace + member
- [ ] **Step 9: Run all service tests**

Run: `cd backend && ./mvnw test -Dtest="com.briefflow.unit.service.*" -pl .`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/briefflow/service/ backend/src/test/java/com/briefflow/unit/service/
git commit -m "feat: add WorkspaceService, MemberService, update AuthService for workspace creation"
```

---

## Task B8: Controllers + SecurityConfig + integration tests

**Files:**
- Create: `backend/src/main/java/com/briefflow/controller/WorkspaceController.java`
- Create: `backend/src/main/java/com/briefflow/controller/MemberController.java`
- Create: `backend/src/main/java/com/briefflow/controller/InviteController.java`
- Modify: `backend/src/main/java/com/briefflow/config/SecurityConfig.java`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/test/java/com/briefflow/integration/controller/MemberControllerTest.java`
- Create: `backend/src/test/java/com/briefflow/integration/controller/InviteControllerTest.java`

All controller code and integration test code is in the backend architect deliverable (Sections 7, 8, and 9 — B8 tests).

SecurityConfig change: add `.requestMatchers("/api/v1/invite/**").permitAll()`
application.yml change: add `app.frontend-url: ${FRONTEND_URL:http://localhost:4200}`

- [ ] **Step 1: Implement WorkspaceController** (GET + PUT /api/v1/workspace)
- [ ] **Step 2: Implement MemberController** (GET /members, POST /members/invite, DELETE /members/{id}, PATCH /members/{id}/role)
- [ ] **Step 3: Implement InviteController** (GET /invite/{token}, POST /invite/{token}/accept — PUBLIC)
- [ ] **Step 4: Update SecurityConfig** — add `/api/v1/invite/**` to permitAll
- [ ] **Step 5: Update application.yml** — add `app.frontend-url`
- [ ] **Step 6: Write MemberControllerTest** (integration with Testcontainers)
- [ ] **Step 7: Write InviteControllerTest** (integration with Testcontainers)
- [ ] **Step 8: Run all backend tests**

Run: `cd backend && ./mvnw test -Dtest="com.briefflow.unit.**" -pl .`
Expected: PASS (integration tests require Docker)

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/briefflow/controller/ backend/src/main/java/com/briefflow/config/SecurityConfig.java backend/src/main/resources/application.yml backend/src/test/java/com/briefflow/integration/
git commit -m "feat: add WorkspaceController, MemberController, InviteController with integration tests"
```

---

## Task F1: Frontend Models

**Files:**
- Create: `frontend/src/app/features/members/models/member.model.ts`
- Create: `frontend/src/app/features/settings/models/workspace.model.ts`
- Create: `frontend/src/app/features/auth/models/invite.model.ts`
- Modify: `frontend/src/app/core/models/user.model.ts`

All model code is in the frontend architect deliverable (Section 1).

Key change to `user.model.ts`: add `workspaceName: string` to `RegisterRequest`.

- [ ] **Step 1: Create member.model.ts** (Member, MemberRole, MemberPosition, labels, InviteMemberRequest, InviteResponse)
- [ ] **Step 2: Create workspace.model.ts** (Workspace, UpdateWorkspaceRequest)
- [ ] **Step 3: Create invite.model.ts** (InviteInfo, AcceptInviteRequest)
- [ ] **Step 4: Modify user.model.ts** — add `workspaceName` to RegisterRequest
- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/members/models/ frontend/src/app/features/settings/models/ frontend/src/app/features/auth/models/ frontend/src/app/core/models/user.model.ts
git commit -m "feat: add member, workspace, invite models, update RegisterRequest"
```

---

## Task F2: Frontend Services + tests

**Files:**
- Create: `frontend/src/app/features/members/services/member-api.service.ts`
- Create: `frontend/src/app/features/members/services/member-api.service.spec.ts`
- Create: `frontend/src/app/features/settings/services/workspace-api.service.ts`
- Create: `frontend/src/app/features/settings/services/workspace-api.service.spec.ts`
- Create: `frontend/src/app/features/auth/services/invite-api.service.ts`
- Create: `frontend/src/app/features/auth/services/invite-api.service.spec.ts`

All service and test code is in the frontend architect deliverable (Sections 2 and 8).

- [ ] **Step 1: Create member-api.service.ts + spec** (list, invite, remove, updateRole)
- [ ] **Step 2: Create workspace-api.service.ts + spec** (get, update)
- [ ] **Step 3: Create invite-api.service.ts + spec** (getInfo, accept)
- [ ] **Step 4: Run frontend tests**

Run: `cd frontend && npx ng test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/features/members/services/ frontend/src/app/features/settings/services/ frontend/src/app/features/auth/services/
git commit -m "feat: add member-api, workspace-api, invite-api services with tests"
```

---

## Task F3: Auth interceptor modification + role guard

**Files:**
- Modify: `frontend/src/app/core/interceptors/auth.interceptor.ts`
- Create: `frontend/src/app/core/guards/role.guard.ts`
- Create: `frontend/src/app/core/guards/role.guard.spec.ts`

- [ ] **Step 1: Update auth interceptor** — add `/api/v1/invite/` to public endpoints skip list

Change in `authInterceptor`:
```typescript
if (error.status === 401 && !req.url.includes('/auth/') && !req.url.includes('/api/v1/invite/')) {
```

- [ ] **Step 2: Create role.guard.ts**

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { StorageService } from '../services/storage.service';

export const roleGuard = (...allowedRoles: string[]): CanActivateFn => {
  return () => {
    const storage = inject(StorageService);
    const router = inject(Router);

    const user = storage.getUser();
    const userRole = (user as any)?.role;

    if (userRole && allowedRoles.includes(userRole)) {
      return true;
    }

    return router.createUrlTree(['/dashboard']);
  };
};
```

- [ ] **Step 3: Create role.guard.spec.ts** (code in frontend architect deliverable Section 8.8)
- [ ] **Step 4: Run frontend tests** — Expected: PASS
- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/core/interceptors/auth.interceptor.ts frontend/src/app/core/guards/role.guard.ts frontend/src/app/core/guards/role.guard.spec.ts
git commit -m "feat: update auth interceptor for invite endpoints, add role guard"
```

---

## Task F4: Register page modification

**Files:**
- Modify: `frontend/src/app/features/auth/pages/register/register.component.ts`
- Modify: `frontend/src/app/features/auth/pages/register/register.component.html`

Add `workspaceName` form field. Full code in frontend architect deliverable (Section 3.4).

- [ ] **Step 1: Update register.component.ts** — add `workspaceName` to form, include in register call
- [ ] **Step 2: Update register.component.html** — add "Nome da agência" input field after email
- [ ] **Step 3: Run frontend tests** — Expected: PASS
- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/auth/pages/register/
git commit -m "feat: add workspace name field to register page"
```

---

## Task F5: Invite member dialog + tests

**Files:**
- Create: `frontend/src/app/features/members/components/invite-member-dialog/invite-member-dialog.component.ts`
- Create: `frontend/src/app/features/members/components/invite-member-dialog/invite-member-dialog.component.html`
- Create: `frontend/src/app/features/members/components/invite-member-dialog/invite-member-dialog.component.spec.ts`

Full code in frontend architect deliverable (Section 4.1 and 8.5).

- [ ] **Step 1: Write invite-member-dialog.component.spec.ts** (6 tests)
- [ ] **Step 2: Implement invite-member-dialog.component.ts + .html** (two-state modal: form → link generated)
- [ ] **Step 3: Run frontend tests** — Expected: PASS
- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/members/components/
git commit -m "feat: add invite member dialog component"
```

---

## Task F6: Member list page + tests

**Files:**
- Create: `frontend/src/app/features/members/pages/member-list/member-list.component.ts`
- Create: `frontend/src/app/features/members/pages/member-list/member-list.component.html`
- Create: `frontend/src/app/features/members/pages/member-list/member-list.component.spec.ts`

Full code in frontend architect deliverable (Section 3.1 and 8.4).

- [ ] **Step 1: Write member-list.component.spec.ts** (7 tests)
- [ ] **Step 2: Implement member-list.component.ts + .html** (table with avatar, cargo, papel badge, status, actions)
- [ ] **Step 3: Run frontend tests** — Expected: PASS
- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/members/pages/
git commit -m "feat: add member list page with table and invite integration"
```

---

## Task F7: Accept invite page + tests

**Files:**
- Create: `frontend/src/app/features/auth/pages/accept-invite/accept-invite.component.ts`
- Create: `frontend/src/app/features/auth/pages/accept-invite/accept-invite.component.html`
- Create: `frontend/src/app/features/auth/pages/accept-invite/accept-invite.component.spec.ts`

Full code in frontend architect deliverable (Section 3.2 and 8.6).

- [ ] **Step 1: Write accept-invite.component.spec.ts** (5 tests)
- [ ] **Step 2: Implement accept-invite.component.ts + .html** (smart form: detects userExists)
- [ ] **Step 3: Run frontend tests** — Expected: PASS
- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/auth/pages/accept-invite/
git commit -m "feat: add accept invite page with smart form (new/existing user)"
```

---

## Task F8: Settings page + tests

**Files:**
- Create: `frontend/src/app/features/settings/pages/settings/settings.component.ts`
- Create: `frontend/src/app/features/settings/pages/settings/settings.component.html`
- Create: `frontend/src/app/features/settings/pages/settings/settings.component.spec.ts`

Full code in frontend architect deliverable (Section 3.3 and 8.7).

- [ ] **Step 1: Write settings.component.spec.ts** (4 tests)
- [ ] **Step 2: Implement settings.component.ts + .html** (edit workspace name)
- [ ] **Step 3: Run frontend tests** — Expected: PASS
- [ ] **Step 4: Commit**

```bash
git add frontend/src/app/features/settings/
git commit -m "feat: add settings page with workspace name editing"
```

---

## Task F9: Routes

**Files:**
- Create: `frontend/src/app/features/members/members.routes.ts`
- Create: `frontend/src/app/features/settings/settings.routes.ts`
- Modify: `frontend/src/app/features/auth/auth.routes.ts`
- Modify: `frontend/src/app/app.routes.ts`

Full code in frontend architect deliverable (Section 5).

- [ ] **Step 1: Create members.routes.ts** (lazy load MemberListComponent)
- [ ] **Step 2: Create settings.routes.ts** (lazy load SettingsComponent)
- [ ] **Step 3: Modify auth.routes.ts** — add `accept-invite` route
- [ ] **Step 4: Modify app.routes.ts** — add `members` and `settings` routes under authGuard
- [ ] **Step 5: Run all frontend tests**

Run: `cd frontend && npx ng test`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add frontend/src/app/features/members/members.routes.ts frontend/src/app/features/settings/settings.routes.ts frontend/src/app/features/auth/auth.routes.ts frontend/src/app/app.routes.ts
git commit -m "feat: wire up members, settings, accept-invite routes"
```

---

## Post-Implementation Verification

After all tasks complete:

1. **Run all backend tests:**
   ```bash
   cd backend && ./mvnw test -Dtest="com.briefflow.unit.**"
   ```
   Expected: All unit tests pass.

2. **Run all frontend tests:**
   ```bash
   cd frontend && npx ng test
   ```
   Expected: All tests pass.

3. **Manual smoke test:**
   - Start Docker + backend + frontend
   - Register with workspace name → verify workspace created
   - Go to /members → verify member list shows OWNER
   - Invite a member → copy link
   - Open link in incognito → accept invite as new user
   - Verify new member appears in list
   - Test removing a member
   - Go to /settings → edit workspace name
