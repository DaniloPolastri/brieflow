package com.briefflow.unit.service;

import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Job;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.entity.Workspace;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.ForbiddenException;
import com.briefflow.exception.ResourceNotFoundException;
import com.briefflow.mapper.JobMapper;
import com.briefflow.repository.ClientMemberRepository;
import com.briefflow.repository.ClientRepository;
import com.briefflow.repository.JobFileRepository;
import com.briefflow.repository.JobRepository;
import com.briefflow.repository.MemberRepository;
import com.briefflow.repository.UserRepository;
import com.briefflow.repository.WorkspaceRepository;
import com.briefflow.service.FileStorageService;
import com.briefflow.service.briefing.BriefingValidator;
import com.briefflow.service.impl.JobServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobFileRepository jobFileRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private ClientMemberRepository clientMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private JobMapper jobMapper;
    @Mock private BriefingValidator briefingValidator;
    @Mock private FileStorageService fileStorageService;

    private JobServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new JobServiceImpl(
            jobRepository, jobFileRepository, clientRepository, memberRepository,
            clientMemberRepository, userRepository, workspaceRepository,
            jobMapper, briefingValidator, fileStorageService
        );
    }

    @Test
    void should_createJob_when_callerIsManager_andBriefingValid() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(clientRepository.findByIdAndWorkspaceId(100L, workspaceId)).thenReturn(Optional.of(createClient(100L, workspaceId)));
        when(userRepository.findById(userId)).thenReturn(Optional.of(manager.getUser()));
        when(jobRepository.incrementAndGetJobCounter(workspaceId)).thenReturn(1L);
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> {
            Job j = inv.getArgument(0); j.setId(500L); return j;
        });
        when(jobMapper.toResponseDTO(any(Job.class))).thenReturn(mock(JobResponseDTO.class));

        JobRequestDTO req = new JobRequestDTO(100L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));
        JobResponseDTO dto = service.createJob(workspaceId, userId, req);

        assertNotNull(dto);
        verify(briefingValidator).validate(JobType.POST_FEED, req.briefingData());
        verify(jobRepository).save(any(Job.class));
    }

    @Test
    void should_throwForbidden_when_creativeTriesToCreateJob() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));

        JobRequestDTO req = new JobRequestDTO(100L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));

        assertThrows(ForbiddenException.class, () -> service.createJob(workspaceId, userId, req));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void should_throwNotFound_when_clientDoesNotBelongToWorkspace() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(clientRepository.findByIdAndWorkspaceId(999L, workspaceId)).thenReturn(Optional.empty());

        JobRequestDTO req = new JobRequestDTO(999L, null, "T", JobType.POST_FEED, JobPriority.NORMAL, null, null,
                Map.of("captionText", "c", "format", "1:1"));

        assertThrows(ResourceNotFoundException.class, () -> service.createJob(workspaceId, userId, req));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_listAllJobs_when_callerIsManager() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(jobRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(new Job(), new Job()));
        when(jobMapper.toListItemDTOList(anyList())).thenReturn(List.of());

        service.listJobs(workspaceId, userId, null, null, null, null, null, false, null);

        verify(jobRepository).findAll(any(Specification.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_onlyReturnVisibleJobs_when_callerIsCreative() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));
        when(jobRepository.findVisibleToCreative(workspaceId, creative.getId(), false)).thenReturn(List.of());
        when(jobMapper.toListItemDTOList(anyList())).thenReturn(List.of());

        service.listJobs(workspaceId, userId, null, null, null, null, null, false, null);

        verify(jobRepository).findVisibleToCreative(workspaceId, creative.getId(), false);
        verify(jobRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void should_archiveJob_when_callerIsManager() {
        Long userId = 1L, workspaceId = 2L;
        Member manager = createMember(10L, userId, workspaceId, MemberRole.MANAGER);
        Job job = new Job(); job.setId(500L); job.setStatus(JobStatus.NOVO);
        Workspace w = new Workspace(); w.setId(workspaceId); job.setWorkspace(w);

        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(manager));
        when(jobRepository.findByIdAndWorkspaceId(500L, workspaceId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobMapper.toResponseDTO(any(Job.class))).thenReturn(mock(JobResponseDTO.class));

        service.archiveJob(workspaceId, userId, 500L, true);

        assertTrue(job.getArchived());
        verify(jobRepository).save(job);
    }

    @Test
    void should_throwForbidden_when_creativeTriesToArchive() {
        Long userId = 1L, workspaceId = 2L;
        Member creative = createMember(10L, userId, workspaceId, MemberRole.CREATIVE);
        when(memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)).thenReturn(Optional.of(creative));

        assertThrows(ForbiddenException.class, () -> service.archiveJob(workspaceId, userId, 500L, true));
    }

    private User createUser(Long id) {
        User u = new User(); u.setId(id); u.setName("U" + id); u.setEmail(id + "@t.com"); return u;
    }
    private Workspace createWorkspace(Long id) {
        Workspace w = new Workspace(); w.setId(id); w.setName("WS"); return w;
    }
    private Member createMember(Long id, Long userId, Long workspaceId, MemberRole role) {
        Member m = new Member(); m.setId(id);
        m.setUser(createUser(userId)); m.setWorkspace(createWorkspace(workspaceId));
        m.setRole(role); return m;
    }
    private Client createClient(Long id, Long workspaceId) {
        Client c = new Client(); c.setId(id); c.setName("C" + id);
        c.setWorkspace(createWorkspace(workspaceId)); c.setActive(true); return c;
    }
}
