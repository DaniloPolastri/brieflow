package com.briefflow.entity;

import com.briefflow.enums.JobPriority;
import com.briefflow.enums.JobStatus;
import com.briefflow.enums.JobType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "jobs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_jobs_workspace_sequence", columnNames = {"workspace_id", "sequence_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_creative_id")
    private Member assignedCreative;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobPriority priority = JobPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status = JobStatus.NOVO;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate deadline;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "briefing_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> briefingData = new HashMap<>();

    @Column(nullable = false)
    private Boolean archived = false;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobFile> files = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getCode() {
        return String.format("JOB-%03d", sequenceNumber);
    }
}
