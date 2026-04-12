package com.briefflow.service.impl;

import com.briefflow.dto.job.JobFileDTO;
import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobRequestDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.dto.job.JobStatusEvent;
import com.briefflow.dto.job.JobStatusResponseDTO;
import com.briefflow.dto.job.UpdateJobStatusDTO;
import com.briefflow.entity.Client;
import com.briefflow.entity.Job;
import com.briefflow.entity.JobFile;
import com.briefflow.entity.Member;
import com.briefflow.entity.User;
import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import com.briefflow.enums.MemberRole;
import com.briefflow.exception.BusinessException;
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
import com.briefflow.service.JobService;
import com.briefflow.service.JobSseService;
import com.briefflow.service.briefing.BriefingValidator;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.briefflow.repository.JobSpecifications.assignedCreative;
import static com.briefflow.repository.JobSpecifications.forClient;
import static com.briefflow.repository.JobSpecifications.hasArchived;
import static com.briefflow.repository.JobSpecifications.hasPriority;
import static com.briefflow.repository.JobSpecifications.hasStatus;
import static com.briefflow.repository.JobSpecifications.hasType;
import static com.briefflow.repository.JobSpecifications.inWorkspace;
import static com.briefflow.repository.JobSpecifications.titleOrCodeContains;

@Service
public class JobServiceImpl implements JobService {

    private static final Set<String> ALLOWED_MIMES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif",
        "application/pdf", "video/mp4", "video/quicktime"
    );
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final ClientRepository clientRepository;
    private final MemberRepository memberRepository;
    private final ClientMemberRepository clientMemberRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final JobMapper jobMapper;
    private final BriefingValidator briefingValidator;
    private final FileStorageService fileStorageService;
    private final JobSseService jobSseService;

    public JobServiceImpl(JobRepository jobRepository, JobFileRepository jobFileRepository,
                          ClientRepository clientRepository, MemberRepository memberRepository,
                          ClientMemberRepository clientMemberRepository, UserRepository userRepository,
                          WorkspaceRepository workspaceRepository, JobMapper jobMapper,
                          BriefingValidator briefingValidator, FileStorageService fileStorageService,
                          JobSseService jobSseService) {
        this.jobRepository = jobRepository;
        this.jobFileRepository = jobFileRepository;
        this.clientRepository = clientRepository;
        this.memberRepository = memberRepository;
        this.clientMemberRepository = clientMemberRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.jobMapper = jobMapper;
        this.briefingValidator = briefingValidator;
        this.fileStorageService = fileStorageService;
        this.jobSseService = jobSseService;
    }

    @Override
    @Transactional
    public JobResponseDTO createJob(Long workspaceId, Long userId, JobRequestDTO req) {
        Member caller = requireOwnerOrManager(userId, workspaceId);
        briefingValidator.validate(req.type(), req.briefingData());

        Client client = clientRepository.findByIdAndWorkspaceId(req.clientId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        if (client.getActive() == null || !client.getActive()) {
            throw new BusinessException("Cliente está inativo");
        }

        Member assignedCreative = null;
        if (req.assignedCreativeId() != null) {
            assignedCreative = memberRepository.findByIdAndWorkspaceId(req.assignedCreativeId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membro designado não encontrado"));
            validateAssignedCreative(assignedCreative, req.clientId());
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Long sequenceNumber = jobRepository.incrementAndGetJobCounter(workspaceId);

        Job job = new Job();
        job.setWorkspace(caller.getWorkspace());
        job.setClient(client);
        job.setAssignedCreative(assignedCreative);
        job.setCreatedBy(creator);
        job.setSequenceNumber(sequenceNumber.intValue());
        job.setTitle(req.title());
        job.setType(req.type());
        job.setPriority(req.priority());
        job.setStatus(JobStatus.NOVO);
        job.setDescription(req.description());
        job.setDeadline(req.deadline());
        job.setBriefingData(req.briefingData());

        Job saved = jobRepository.save(job);
        return jobMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobListItemDTO> listJobs(Long workspaceId, Long userId,
                                         JobStatus status, JobType type, JobPriority priority,
                                         Long clientId, Long assignedCreativeId,
                                         Boolean archived, String search) {
        Member caller = requireMember(userId, workspaceId);
        boolean archivedFlag = archived != null && archived;
        List<Job> jobs;
        if (caller.getRole() == MemberRole.CREATIVE) {
            // CREATIVE path uses a dedicated JPQL method for visibility.
            // Filters are applied in-memory after the fetch.
            // TODO: push filters down to SQL in v2 when the dataset grows.
            jobs = jobRepository.findVisibleToCreative(workspaceId, caller.getId(), archivedFlag);
            jobs = applyFiltersInMemory(jobs, status, type, priority, clientId, assignedCreativeId, search);
        } else {
            Specification<Job> spec = Specification.where(inWorkspace(workspaceId))
                    .and(hasArchived(archived))
                    .and(hasStatus(status))
                    .and(hasType(type))
                    .and(hasPriority(priority))
                    .and(forClient(clientId))
                    .and(assignedCreative(assignedCreativeId))
                    .and(titleOrCodeContains(search));
            jobs = jobRepository.findAll(spec);
        }
        return jobMapper.toListItemDTOList(jobs);
    }

    private List<Job> applyFiltersInMemory(List<Job> jobs,
                                           JobStatus status, JobType type, JobPriority priority,
                                           Long clientId, Long assignedCreativeId, String search) {
        String s = (search == null || search.isBlank()) ? null : search.toLowerCase();
        return jobs.stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> type == null || j.getType() == type)
                .filter(j -> priority == null || j.getPriority() == priority)
                .filter(j -> clientId == null
                        || (j.getClient() != null && clientId.equals(j.getClient().getId())))
                .filter(j -> assignedCreativeId == null
                        || (j.getAssignedCreative() != null
                            && assignedCreativeId.equals(j.getAssignedCreative().getId())))
                .filter(j -> s == null
                        || (j.getTitle() != null && j.getTitle().toLowerCase().contains(s))
                        || (j.getSequenceNumber() != null
                            && String.valueOf(j.getSequenceNumber()).contains(s)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponseDTO getJob(Long workspaceId, Long userId, Long jobId) {
        Member caller = requireMember(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceIdWithDetails(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        assertCanView(caller, job);
        return jobMapper.toResponseDTO(job);
    }

    @Override
    @Transactional
    public JobResponseDTO updateJob(Long workspaceId, Long userId, Long jobId, JobRequestDTO req) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));

        briefingValidator.validate(req.type(), req.briefingData());

        if (!job.getClient().getId().equals(req.clientId())) {
            Client client = clientRepository.findByIdAndWorkspaceId(req.clientId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
            if (client.getActive() == null || !client.getActive()) {
                throw new BusinessException("Cliente está inativo");
            }
            job.setClient(client);
        }

        if (req.assignedCreativeId() == null) {
            job.setAssignedCreative(null);
        } else if (job.getAssignedCreative() == null
                || !job.getAssignedCreative().getId().equals(req.assignedCreativeId())) {
            Member m = memberRepository.findByIdAndWorkspaceId(req.assignedCreativeId(), workspaceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Membro não encontrado"));
            validateAssignedCreative(m, req.clientId());
            job.setAssignedCreative(m);
        } else {
            // Same creative, but client may have changed — re-validate membership
            validateAssignedCreative(job.getAssignedCreative(), req.clientId());
        }

        job.setTitle(req.title());
        job.setType(req.type());
        job.setPriority(req.priority());
        job.setDescription(req.description());
        job.setDeadline(req.deadline());
        job.setBriefingData(req.briefingData());

        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    @Override
    @Transactional
    public JobResponseDTO archiveJob(Long workspaceId, Long userId, Long jobId, boolean archived) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        job.setArchived(archived);
        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    @Override
    @Transactional
    public JobFileDTO uploadFile(Long workspaceId, Long userId, Long jobId, MultipartFile file) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));

        if (file.isEmpty()) {
            throw new BusinessException("Arquivo vazio");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("Arquivo excede 50MB");
        }
        String mime = file.getContentType();
        if (mime == null || !ALLOWED_MIMES.contains(mime)) {
            throw new BusinessException("Tipo de arquivo não permitido: " + mime);
        }

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String stored = UUID.randomUUID() + ext;

        fileStorageService.store(file, "jobs/" + jobId, stored);

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        JobFile jf = new JobFile();
        jf.setJob(job);
        jf.setOriginalFilename(original);
        jf.setStoredFilename(stored);
        jf.setMimeType(mime);
        jf.setSizeBytes(file.getSize());
        jf.setUploadedBy(uploader);
        job.getFiles().add(jf);
        JobFile saved = jobFileRepository.save(jf);

        return jobMapper.toFileDTO(saved);
    }

    @Override
    @Transactional
    public void deleteFile(Long workspaceId, Long userId, Long jobId, Long fileId) {
        requireOwnerOrManager(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        JobFile file = jobFileRepository.findByIdAndJobId(fileId, job.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        fileStorageService.delete("/uploads/jobs/" + job.getId() + "/" + file.getStoredFilename());
        jobFileRepository.delete(file);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(Long workspaceId, Long userId, Long jobId, Long fileId) {
        Member caller = requireMember(userId, workspaceId);
        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job não encontrado"));
        assertCanView(caller, job);
        JobFile file = jobFileRepository.findByIdAndJobId(fileId, job.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        return fileStorageService.load("/uploads/jobs/" + job.getId() + "/" + file.getStoredFilename());
    }

    @Override
    @Transactional
    public JobStatusResponseDTO updateJobStatus(Long workspaceId, Long userId, Long jobId, UpdateJobStatusDTO dto) {
        Member caller = memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Membro nao encontrado"));

        Job job = jobRepository.findByIdAndWorkspaceId(jobId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Job nao encontrado"));

        // Permission: CREATIVE can only move their own assigned jobs
        if (caller.getRole() == MemberRole.CREATIVE) {
            if (job.getAssignedCreative() == null || !job.getAssignedCreative().getId().equals(caller.getId())) {
                throw new ForbiddenException("Criativos so podem mover jobs atribuidos a eles");
            }
        }

        JobStatus previousStatus = job.getStatus();
        JobStatus newStatus = dto.status();

        // Forward skip detection
        boolean skippedSteps = newStatus.ordinal() > previousStatus.ordinal() + 1;

        if (skippedSteps && !dto.confirm()) {
            return new JobStatusResponseDTO(
                    job.getId(),
                    job.getCode(),
                    previousStatus,
                    newStatus,
                    true,
                    false
            );
        }

        // Apply the status change
        job.setStatus(newStatus);
        jobRepository.save(job);

        // Emit SSE event
        JobStatusEvent event = new JobStatusEvent(job.getId(), previousStatus, newStatus);
        jobSseService.publish(job.getClient().getId(), event);

        return new JobStatusResponseDTO(
                job.getId(),
                job.getCode(),
                previousStatus,
                newStatus,
                skippedSteps,
                true
        );
    }

    private void validateAssignedCreative(Member assignedCreative, Long clientId) {
        if (assignedCreative.getRole() != MemberRole.CREATIVE) {
            throw new BusinessException("Membro designado deve ser CREATIVE");
        }
        if (!clientMemberRepository.existsByClientIdAndMemberId(clientId, assignedCreative.getId())) {
            throw new BusinessException("Criativo não está atribuído a este cliente");
        }
    }

    private Member requireMember(Long userId, Long workspaceId) {
        return memberRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
                .orElseThrow(() -> new ForbiddenException("Usuário não pertence ao workspace"));
    }

    private Member requireOwnerOrManager(Long userId, Long workspaceId) {
        Member m = requireMember(userId, workspaceId);
        if (m.getRole() == MemberRole.CREATIVE) {
            throw new ForbiddenException("Operação restrita a OWNER/MANAGER");
        }
        return m;
    }

    private void assertCanView(Member caller, Job job) {
        if (caller.getRole() != MemberRole.CREATIVE) {
            return;
        }
        boolean assigned = job.getAssignedCreative() != null
                && job.getAssignedCreative().getId().equals(caller.getId());
        boolean clientMember = clientMemberRepository.existsByClientIdAndMemberId(
                job.getClient().getId(), caller.getId());
        if (!assigned && !clientMember) {
            throw new ForbiddenException("Sem acesso ao job");
        }
    }
}
