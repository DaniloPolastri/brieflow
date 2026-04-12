package com.briefflow.unit.service;

import com.briefflow.dto.job.JobStatusEvent;
import com.briefflow.enums.JobStatus;
import com.briefflow.service.JobSseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class JobSseServiceTest {

    private JobSseService jobSseService;

    @BeforeEach
    void setUp() {
        jobSseService = new JobSseService();
    }

    @Test
    void should_returnNewSseEmitter_when_subscribe() {
        SseEmitter emitter = jobSseService.subscribe(1L);
        assertNotNull(emitter);
    }

    @Test
    void should_publishToSubscribedEmitters_when_eventSent() {
        // Subscribe creates an emitter — no exception on publish
        jobSseService.subscribe(1L);
        JobStatusEvent event = new JobStatusEvent(10L, JobStatus.NOVO, JobStatus.EM_CRIACAO);
        assertDoesNotThrow(() -> jobSseService.publish(1L, event));
    }

    @Test
    void should_notThrow_when_publishToClientWithNoSubscribers() {
        JobStatusEvent event = new JobStatusEvent(10L, JobStatus.NOVO, JobStatus.EM_CRIACAO);
        assertDoesNotThrow(() -> jobSseService.publish(999L, event));
    }

    @Test
    void should_heartbeatAllEmitters_withoutError() {
        jobSseService.subscribe(1L);
        jobSseService.subscribe(2L);
        assertDoesNotThrow(() -> jobSseService.heartbeat());
    }
}
