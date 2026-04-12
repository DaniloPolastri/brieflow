package com.briefflow.service;

import com.briefflow.dto.job.JobStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class JobSseService {

    private static final Logger log = LoggerFactory.getLogger(JobSseService.class);
    private static final long SSE_TIMEOUT = 60_000L;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long clientId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable removeCallback = () -> removeEmitter(clientId, emitter);
        emitter.onCompletion(removeCallback);
        emitter.onTimeout(removeCallback);
        emitter.onError(e -> removeCallback.run());

        return emitter;
    }

    public void publish(Long clientId, JobStatusEvent event) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters == null) {
            return;
        }

        for (SseEmitter emitter : clientEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("job-status-changed")
                        .data(event));
            } catch (IOException e) {
                log.debug("Removing failed SSE emitter for client {}", clientId);
                removeEmitter(clientId, emitter);
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void heartbeat() {
        emitters.forEach((clientId, clientEmitters) -> {
            for (SseEmitter emitter : clientEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    log.debug("Removing dead SSE emitter for client {} during heartbeat", clientId);
                    removeEmitter(clientId, emitter);
                }
            }
        });
    }

    private void removeEmitter(Long clientId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> clientEmitters = emitters.get(clientId);
        if (clientEmitters != null) {
            clientEmitters.remove(emitter);
            if (clientEmitters.isEmpty()) {
                emitters.remove(clientId);
            }
        }
    }
}
