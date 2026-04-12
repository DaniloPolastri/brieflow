package com.briefflow.integration.controller;

import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobType;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobControllerTest {

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
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JobFileRepository jobFileRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private ClientMemberRepository clientMemberRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private String managerToken;
    private Long workspaceId;
    private Long clientId;

    /**
     * Provisions a fresh OWNER + workspace + client per test by inserting
     * directly through the repositories and minting a JWT via {@link JwtService}.
     * We deliberately avoid calling {@code /api/v1/auth/register} because it is
     * rate-limited to 5 requests/min per IP, which would fail after a handful of
     * tests run against the same Spring context.
     */
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
        workspace.setName("Jobs Agency");
        workspace.setSlug("jobs-agency");
        workspace = workspaceRepository.save(workspace);
        workspaceId = workspace.getId();

        User owner = new User();
        owner.setName("Owner One");
        owner.setEmail("owner@jobs-test.com");
        owner.setPassword(passwordEncoder.encode("password123"));
        owner = userRepository.save(owner);

        Member ownerMember = new Member();
        ownerMember.setUser(owner);
        ownerMember.setWorkspace(workspace);
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setPosition(MemberPosition.DIRETOR_DE_ARTE);
        memberRepository.save(ownerMember);

        managerToken = jwtService.generateAccessToken(owner.getId(), owner.getEmail(), workspaceId);

        Client client = new Client();
        client.setName("Acme Corp");
        client.setWorkspace(workspace);
        client.setActive(true);
        clientId = clientRepository.save(client).getId();
    }

    @Test
    void should_createJob_201_when_managerWithValidPayload() throws Exception {
        JobRequestDTO request = new JobRequestDTO(
                clientId,
                null,
                "Post Black Friday",
                JobType.POST_FEED,
                JobPriority.NORMAL,
                "Promo de BF",
                LocalDate.now().plusDays(5),
                Map.of("captionText", "BF começou!", "format", "1:1")
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("JOB-001"))
                .andExpect(jsonPath("$.title").value("Post Black Friday"))
                .andExpect(jsonPath("$.type").value("POST_FEED"))
                .andExpect(jsonPath("$.status").value("NOVO"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.client.id").value(clientId))
                .andExpect(jsonPath("$.briefingData.captionText").value("BF começou!"));
    }

    @Test
    void should_returnJobsList_200_when_get() throws Exception {
        createJob("First job");
        createJob("Second job");

        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientName").value("Acme Corp"))
                .andExpect(jsonPath("$[0].isOverdue").value(false));
    }

    @Test
    void should_filterByTitle_when_searchParamProvided() throws Exception {
        createJob("Post A");
        createJob("Post B");

        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("search", "Post A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Post A"));
    }

    @Test
    void should_returnArchivedJobs_when_archivedTrue() throws Exception {
        MvcResult active = createJob("Active job");
        MvcResult toArchive = createJob("Archived job");
        Long archivedId = extractId(toArchive);

        mockMvc.perform(patch("/api/v1/jobs/" + archivedId + "/archive")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"archived\":true}"))
                .andExpect(status().isOk());

        // Default (archived=false) returns only active job
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Active job"));

        // archived=true returns only archived job
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Archived job"));

        // sanity: suppress unused-variable warning
        assert active != null;
    }

    @Test
    void should_returnJobDetail_200_when_getById() throws Exception {
        MvcResult create = createJob("Detail job");
        Long jobId = extractId(create);

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(jobId))
                .andExpect(jsonPath("$.title").value("Detail job"))
                .andExpect(jsonPath("$.code").value("JOB-001"));
    }

    @Test
    void should_archiveJob_200_when_patchArchive() throws Exception {
        MvcResult create = createJob("Archivable job");
        Long jobId = extractId(create);

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/archive")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"archived\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void should_return403_when_creativeTriesCreate() throws Exception {
        // Create a CREATIVE user directly attached to the existing workspace
        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        User creativeUser = new User();
        creativeUser.setName("Creative User");
        creativeUser.setEmail("creative@jobs-test.com");
        creativeUser.setPassword(passwordEncoder.encode("password123"));
        creativeUser = userRepository.save(creativeUser);

        Member creativeMember = new Member();
        creativeMember.setUser(creativeUser);
        creativeMember.setWorkspace(workspace);
        creativeMember.setRole(MemberRole.CREATIVE);
        creativeMember.setPosition(MemberPosition.DESIGNER_GRAFICO);
        memberRepository.save(creativeMember);

        String creativeToken = jwtService.generateAccessToken(
                creativeUser.getId(), creativeUser.getEmail(), workspaceId);

        JobRequestDTO request = new JobRequestDTO(
                clientId,
                null,
                "Should be forbidden",
                JobType.POST_FEED,
                JobPriority.NORMAL,
                null,
                null,
                Map.of("captionText", "nope", "format", "1:1")
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + creativeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_return422_when_briefingInvalid() throws Exception {
        // POST_FEED briefing is missing the required "captionText" field
        JobRequestDTO request = new JobRequestDTO(
                clientId,
                null,
                "Invalid briefing",
                JobType.POST_FEED,
                JobPriority.NORMAL,
                null,
                null,
                Map.of("format", "1:1")
        );

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- RF05: PATCH /status tests ----------

    @Test
    void should_patchStatus_200() throws Exception {
        MvcResult create = createJob("Status test job");
        Long jobId = extractId(create);

        String body = """
            {"status": "EM_CRIACAO", "confirm": false}
            """;

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied").value(true))
                .andExpect(jsonPath("$.newStatus").value("EM_CRIACAO"))
                .andExpect(jsonPath("$.previousStatus").value("NOVO"))
                .andExpect(jsonPath("$.skippedSteps").value(false));
    }

    @Test
    void should_patchStatus_return_skippedSteps() throws Exception {
        MvcResult create = createJob("Skip test job");
        Long jobId = extractId(create);

        // Job is NOVO, move to REVISAO_INTERNA (skip EM_CRIACAO)
        String body = """
            {"status": "REVISAO_INTERNA", "confirm": false}
            """;

        mockMvc.perform(patch("/api/v1/jobs/" + jobId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skippedSteps").value(true))
                .andExpect(jsonPath("$.applied").value(false));
    }

    // ---------- helpers ----------

    private MvcResult createJob(String title) throws Exception {
        JobRequestDTO request = new JobRequestDTO(
                clientId,
                null,
                title,
                JobType.POST_FEED,
                JobPriority.NORMAL,
                null,
                LocalDate.now().plusDays(5),
                Map.of("captionText", "caption for " + title, "format", "1:1")
        );

        return mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private Long extractId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}
