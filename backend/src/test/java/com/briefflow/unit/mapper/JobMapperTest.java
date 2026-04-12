package com.briefflow.unit.mapper;

import com.briefflow.dto.job.JobListItemDTO;
import com.briefflow.dto.job.JobResponseDTO;
import com.briefflow.entity.*;
import com.briefflow.enums.*;
import com.briefflow.mapper.JobMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobMapperTest {

    private final JobMapper mapper = Mappers.getMapper(JobMapper.class);

    @Test
    void should_mapJobToResponseDTO_withClientAndAssignedCreativeAndFiles() {
        Workspace w = new Workspace(); w.setId(1L); w.setName("WS");
        Client c = new Client(); c.setId(10L); c.setName("Client A"); c.setCompany("Co A");
        User u = new User(); u.setId(20L); u.setName("Alice"); u.setEmail("a@b.com");
        Member m = new Member(); m.setId(30L); m.setUser(u); m.setRole(MemberRole.CREATIVE);

        Job j = new Job();
        j.setId(100L);
        j.setWorkspace(w);
        j.setClient(c);
        j.setAssignedCreative(m);
        j.setCreatedBy(u);
        j.setSequenceNumber(1);
        j.setTitle("Post X");
        j.setType(JobType.POST_FEED);
        j.setPriority(JobPriority.NORMAL);
        j.setStatus(JobStatus.NOVO);
        j.setDescription("Briefing para post de lançamento");
        j.setDeadline(LocalDate.now().plusDays(3));
        Map<String, Object> bd = new HashMap<>();
        bd.put("captionText", "hello");
        j.setBriefingData(bd);
        j.setCreatedAt(LocalDateTime.now());
        j.setUpdatedAt(LocalDateTime.now());

        JobResponseDTO dto = mapper.toResponseDTO(j);

        assertEquals(100L, dto.id());
        assertEquals("JOB-001", dto.code());
        assertEquals("Post X", dto.title());
        assertEquals(JobType.POST_FEED, dto.type());
        assertEquals("Briefing para post de lançamento", dto.description());
        assertEquals(10L, dto.client().id());
        assertEquals("Client A", dto.client().name());
        assertEquals(30L, dto.assignedCreative().id());
        assertEquals("Alice", dto.assignedCreative().name());
        assertEquals("Alice", dto.createdByName());
        assertEquals("hello", dto.briefingData().get("captionText"));
        assertNotNull(dto.createdAt());
        assertFalse(dto.overdue());
    }

    @Test
    void should_markOverdue_when_deadlineInPastAndStatusNotApproved() {
        Job j = jobWithDeadline(LocalDate.now().minusDays(1), JobStatus.EM_CRIACAO);
        JobResponseDTO dto = mapper.toResponseDTO(j);
        assertTrue(dto.overdue());
    }

    @Test
    void should_notMarkOverdue_when_statusIsAprovado() {
        Job j = jobWithDeadline(LocalDate.now().minusDays(5), JobStatus.APROVADO);
        JobResponseDTO dto = mapper.toResponseDTO(j);
        assertFalse(dto.overdue());
    }

    @Test
    void should_mapJobToListItemDTO() {
        Job j = jobWithDeadline(LocalDate.now().plusDays(1), JobStatus.NOVO);
        JobListItemDTO item = mapper.toListItemDTO(j);
        assertEquals(j.getId(), item.id());
        assertEquals("JOB-001", item.code());
        assertEquals(j.getTitle(), item.title());
        assertEquals("C", item.clientName());
        assertEquals("U", item.assignedCreativeName());
        assertFalse(item.isOverdue());
    }

    private Job jobWithDeadline(LocalDate deadline, JobStatus status) {
        Workspace w = new Workspace(); w.setId(1L);
        Client c = new Client(); c.setId(10L); c.setName("C");
        User u = new User(); u.setId(20L); u.setName("U"); u.setEmail("u@u.com");
        Member m = new Member(); m.setId(30L); m.setUser(u); m.setRole(MemberRole.CREATIVE);
        Job j = new Job();
        j.setId(100L); j.setWorkspace(w); j.setClient(c); j.setAssignedCreative(m); j.setCreatedBy(u);
        j.setSequenceNumber(1); j.setTitle("T"); j.setType(JobType.POST_FEED);
        j.setPriority(JobPriority.NORMAL); j.setStatus(status); j.setDeadline(deadline);
        j.setCreatedAt(LocalDateTime.now()); j.setUpdatedAt(LocalDateTime.now());
        return j;
    }
}
