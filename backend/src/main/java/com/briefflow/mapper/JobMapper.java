package com.briefflow.mapper;

import com.briefflow.dto.job.*;
import com.briefflow.entity.Job;
import com.briefflow.entity.JobFile;
import com.briefflow.entity.Member;
import com.briefflow.enums.JobStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "client", source = "client")
    @Mapping(target = "assignedCreative", source = "assignedCreative")
    @Mapping(target = "createdByName", expression = "java(job.getCreatedBy() != null ? job.getCreatedBy().getName() : null)")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "formatDate")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "overdue", expression = "java(isOverdue(job))")
    JobResponseDTO toResponseDTO(Job job);

    @Mapping(target = "code", expression = "java(job.getCode())")
    @Mapping(target = "clientName", source = "client.name")
    @Mapping(target = "assignedCreativeId", expression = "java(job.getAssignedCreative() != null && job.getAssignedCreative().getUser() != null ? job.getAssignedCreative().getUser().getId() : null)")
    @Mapping(target = "assignedCreativeName", expression = "java(job.getAssignedCreative() != null && job.getAssignedCreative().getUser() != null ? job.getAssignedCreative().getUser().getName() : null)")
    @Mapping(target = "deadline", source = "deadline", qualifiedByName = "formatDate")
    @Mapping(target = "isOverdue", expression = "java(isOverdue(job))")
    JobListItemDTO toListItemDTO(Job job);

    List<JobListItemDTO> toListItemDTOList(List<Job> jobs);

    @Mapping(target = "originalFilename", source = "originalFilename")
    @Mapping(target = "uploadedAt", source = "uploadedAt", qualifiedByName = "formatDateTime")
    @Mapping(target = "downloadUrl", expression = "java(buildDownloadUrl(file))")
    JobFileDTO toFileDTO(JobFile file);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "company", source = "company")
    @Mapping(target = "logoUrl", source = "logoUrl")
    ClientSummaryDTO clientToSummary(com.briefflow.entity.Client client);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "role", source = "role")
    MemberSummaryDTO memberToSummary(Member member);

    default boolean isOverdue(Job job) {
        if (job.getDeadline() == null) return false;
        if (job.getStatus() == JobStatus.APROVADO || job.getStatus() == JobStatus.PUBLICADO) return false;
        return job.getDeadline().isBefore(LocalDate.now());
    }

    default String buildDownloadUrl(JobFile file) {
        if (file == null || file.getJob() == null) return null;
        return "/api/v1/jobs/" + file.getJob().getId() + "/files/" + file.getId() + "/download";
    }

    @Named("formatDate")
    default String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
