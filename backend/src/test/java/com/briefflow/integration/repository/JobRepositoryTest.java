package com.briefflow.integration.repository;

import com.briefflow.entity.Client;
import com.briefflow.entity.ClientMember;
import com.briefflow.entity.Job;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import com.briefflow.enums.MemberPosition;
import com.briefflow.enums.MemberRole;
import com.briefflow.repository.ClientMemberRepository;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.JobRepository;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.UserRepository;
import com.briefflow.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
class JobRepositoryTest {

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

    @Autowired private JobRepository jobRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ClientMemberRepository clientMemberRepository;
    @Autowired private EntityManager entityManager;

    private int userCounter;
    private int workspaceCounter;

    @BeforeEach
    void setUp() {
        userCounter = 0;
        workspaceCounter = 0;
    }

    @Test
    void should_persistAndLoad_jobWithBriefingData() {
        Workspace workspace = newWorkspace("WS-persist");
        User user = newUser();
        Member manager = newMember(workspace, user, MemberRole.MANAGER);
        Client client = newClient(workspace);

        Map<String, Object> briefing = new HashMap<>();
        briefing.put("captionText", "Lançamento Black Friday");
        briefing.put("format", "1:1");
        briefing.put("hashtags", List.of("#bf", "#promo"));

        Job job = newJob(workspace, client, manager, user, 1, "Post BF");
        job.setBriefingData(briefing);
        job.setDescription("Promoção do mês");
        job.setPriority(JobPriority.ALTA);
        Job saved = jobRepository.save(job);

        entityManager.flush();
        entityManager.clear();

        Optional<Job> reloaded = jobRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        Job reloadedJob = reloaded.get();
        assertThat(reloadedJob.getTitle()).isEqualTo("Post BF");
        assertThat(reloadedJob.getCode()).isEqualTo("JOB-001");
        assertThat(reloadedJob.getPriority()).isEqualTo(JobPriority.ALTA);
        assertThat(reloadedJob.getStatus()).isEqualTo(JobStatus.NOVO);
        assertThat(reloadedJob.getArchived()).isFalse();
        assertThat(reloadedJob.getBriefingData()).containsEntry("captionText", "Lançamento Black Friday");
        assertThat(reloadedJob.getBriefingData()).containsEntry("format", "1:1");
        assertThat(reloadedJob.getBriefingData().get("hashtags"))
                .asList()
                .containsExactly("#bf", "#promo");
    }

    @Test
    void should_enforceUniqueSequenceNumberPerWorkspace() {
        Workspace workspace = newWorkspace("WS-unique");
        User user = newUser();
        Member manager = newMember(workspace, user, MemberRole.MANAGER);
        Client client = newClient(workspace);

        Job first = newJob(workspace, client, manager, user, 1, "First");
        jobRepository.saveAndFlush(first);

        Job duplicate = newJob(workspace, client, manager, user, 1, "Duplicate");

        assertThatThrownBy(() -> jobRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_atomicallyIncrementJobCounter() {
        Workspace workspace = newWorkspace("WS-counter");

        Long first = jobRepository.incrementAndGetJobCounter(workspace.getId());
        Long second = jobRepository.incrementAndGetJobCounter(workspace.getId());
        Long third = jobRepository.incrementAndGetJobCounter(workspace.getId());

        assertThat(first).isEqualTo(1L);
        assertThat(second).isEqualTo(2L);
        assertThat(third).isEqualTo(3L);

        entityManager.flush();
        entityManager.clear();

        Workspace reloaded = workspaceRepository.findById(workspace.getId()).orElseThrow();
        assertThat(reloaded.getJobCounter()).isEqualTo(3L);
    }

    @Test
    void should_findVisibleToCreative_whenAssignedToCreative() {
        Workspace workspace = newWorkspace("WS-assigned");
        User owner = newUser();
        User creative = newUser();
        Member managerMember = newMember(workspace, owner, MemberRole.MANAGER);
        Member creativeMember = newMember(workspace, creative, MemberRole.CREATIVE);
        Client client = newClient(workspace);

        Job assignedJob = newJob(workspace, client, creativeMember, owner, 1, "Assigned");
        Job otherJob = newJob(workspace, client, managerMember, owner, 2, "Manager job");
        jobRepository.save(assignedJob);
        jobRepository.save(otherJob);
        entityManager.flush();
        entityManager.clear();

        List<Job> visible = jobRepository.findVisibleToCreative(
                workspace.getId(), creativeMember.getId(), false);

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getTitle()).isEqualTo("Assigned");
    }

    @Test
    void should_findVisibleToCreative_whenCreativeIsClientMember() {
        Workspace workspace = newWorkspace("WS-client-member");
        User owner = newUser();
        User creative = newUser();
        Member managerMember = newMember(workspace, owner, MemberRole.MANAGER);
        Member creativeMember = newMember(workspace, creative, MemberRole.CREATIVE);
        Client client = newClient(workspace);

        ClientMember membership = new ClientMember();
        membership.setClientId(client.getId());
        membership.setMemberId(creativeMember.getId());
        clientMemberRepository.save(membership);

        // Job assigned to the manager, not to the creative — but creative is part of the client
        Job job = newJob(workspace, client, managerMember, owner, 1, "Via client access");
        jobRepository.save(job);
        entityManager.flush();
        entityManager.clear();

        List<Job> visible = jobRepository.findVisibleToCreative(
                workspace.getId(), creativeMember.getId(), false);

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).getTitle()).isEqualTo("Via client access");
    }

    @Test
    void should_notFindJob_whenCreativeHasNoAccess() {
        Workspace workspace = newWorkspace("WS-no-access");
        User owner = newUser();
        User creativeA = newUser();
        User creativeB = newUser();
        Member managerMember = newMember(workspace, owner, MemberRole.MANAGER);
        Member creativeAMember = newMember(workspace, creativeA, MemberRole.CREATIVE);
        Member creativeBMember = newMember(workspace, creativeB, MemberRole.CREATIVE);
        Client client = newClient(workspace);

        Job job = newJob(workspace, client, creativeAMember, owner, 1, "Only A sees this");
        jobRepository.save(job);
        entityManager.flush();
        entityManager.clear();

        List<Job> visibleToB = jobRepository.findVisibleToCreative(
                workspace.getId(), creativeBMember.getId(), false);

        assertThat(visibleToB).isEmpty();

        // sanity check — creative A can still see the job
        List<Job> visibleToA = jobRepository.findVisibleToCreative(
                workspace.getId(), creativeAMember.getId(), false);
        assertThat(visibleToA).hasSize(1);
        assertThat(visibleToA.get(0).getTitle()).isEqualTo("Only A sees this");

        // managerMember reference is not needed here but ensures the workspace has more than one member
        assertThat(managerMember.getId()).isNotNull();
    }

    // ---------- helpers ----------

    private Workspace newWorkspace(String prefix) {
        Workspace ws = new Workspace();
        ws.setName(prefix);
        ws.setSlug(prefix.toLowerCase() + "-" + (++workspaceCounter));
        return workspaceRepository.save(ws);
    }

    private User newUser() {
        User u = new User();
        u.setName("User " + (++userCounter));
        u.setEmail("user" + userCounter + "-" + System.nanoTime() + "@test.local");
        u.setPassword("$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyhash");
        return userRepository.save(u);
    }

    private Member newMember(Workspace workspace, User user, MemberRole role) {
        Member member = new Member();
        member.setUser(user);
        member.setWorkspace(workspace);
        member.setRole(role);
        member.setPosition(MemberPosition.DESIGNER_GRAFICO);
        return memberRepository.save(member);
    }

    private Client newClient(Workspace workspace) {
        Client client = new Client();
        client.setName("Client " + workspace.getName());
        client.setWorkspace(workspace);
        client.setActive(true);
        return clientRepository.save(client);
    }

    private Job newJob(Workspace workspace, Client client, Member assignedCreative,
                       User creator, Integer sequence, String title) {
        Job job = new Job();
        job.setWorkspace(workspace);
        job.setClient(client);
        job.setAssignedCreative(assignedCreative);
        job.setCreatedBy(creator);
        job.setSequenceNumber(sequence);
        job.setTitle(title);
        job.setType(JobType.POST_FEED);
        job.setPriority(JobPriority.NORMAL);
        job.setStatus(JobStatus.NOVO);
        job.setDeadline(LocalDate.now().plusDays(7));
        Map<String, Object> briefing = new HashMap<>();
        briefing.put("captionText", "caption");
        briefing.put("format", "1:1");
        job.setBriefingData(briefing);
        return job;
    }
}
