package com.briefflow.repository;

import com.briefflow.entity.JobFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobFileRepository extends JpaRepository<JobFile, Long> {
    Optional<JobFile> findByIdAndJobId(Long id, Long jobId);
}
