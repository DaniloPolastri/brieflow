package com.briefflow.integration.controller;

import com.briefflow.entity.Client;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.repository.ClientMemberRepository;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.JobFileRepository;
import com.briefflow.repository.JobRepository;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.RefreshTokenRepository;
import com.briefflow.repository.UserRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobSseControllerTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("briefflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JobFileRepository jobFileRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ClientMemberRepository clientMemberRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String validToken;
    private Long clientId;

    @BeforeEach
    void setUp() {
        jobFileRepository.deleteAll();
        jobRepository.deleteAll();
        clientMemberRepository.deleteAll();
        clientRepository.deleteAll();
        memberRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        Workspace workspace = new Workspace();
        workspace.setName("SSE Agency");
        workspace.setSlug("sse-agency");
        workspace = workspaceRepository.save(workspace);

        User user = new User();
        user.setName("SSE User");
        user.setEmail("sse@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user = userRepository.save(user);

        Member member = new Member();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(MemberRole.OWNER);
        member.setPosition(MemberPosition.DIRETOR_DE_ARTE);
        memberRepository.save(member);

        Client client = new Client();
        client.setName("SSE Client");
        client.setWorkspace(workspace);
        client.setActive(true);
        clientId = clientRepository.save(client).getId();

        validToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), workspace.getId());
    }

    @Test
    void should_openSseStream_200() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/jobs/stream", clientId)
                        .param("token", validToken))
                .andExpect(status().isOk());
    }

    @Test
    void should_reject_invalidToken_401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/{clientId}/jobs/stream", clientId)
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_reject_403_when_userNotInWorkspace() throws Exception {
        // Create a second workspace + client that the user does NOT belong to
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("Other Agency");
        otherWorkspace.setSlug("other-agency");
        otherWorkspace = workspaceRepository.save(otherWorkspace);

        Client otherClient = new Client();
        otherClient.setName("Other Client");
        otherClient.setWorkspace(otherWorkspace);
        otherClient.setActive(true);
        Long otherClientId = clientRepository.save(otherClient).getId();

        // Token is from the first workspace, but clientId belongs to another workspace
        mockMvc.perform(get("/api/v1/clients/{clientId}/jobs/stream", otherClientId)
                        .param("token", validToken))
                .andExpect(status().isForbidden());
    }
}
