package com.example.businessplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "projects")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String communityName;
    private String projectName;
    private String projectPeriod;
    private String projectLocation;

    private Long totalBudget;
    private Long provincialFund;
    private Long cityFund;
    private Long selfFund;

    @Column(columnDefinition = "TEXT")
    private String budgetDetails;

    @Column(columnDefinition = "TEXT")
    private String detailedPlan;

    @Column(columnDefinition = "TEXT")
    private String monthlyPlan;

    @Column(columnDefinition = "TEXT")
    private String expectedEffect;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}