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

    // 1. 사업개요 정보
    private String communityName;      // 공동체명
    private String projectName;        // 사업명
    private String projectPeriod;      // 사업기간 (예: 2025. 3. ~ 11.)
    private String projectLocation;    // 사업위치

    // 사업비
    private Long totalBudget;          // 총사업비 (단위: 천원)
    private Long provincialFund;       // 도비
    private Long cityFund;             // 시군비
    private Long selfFund;             // 자부담

    @Column(columnDefinition = "TEXT")
    private String budgetDetails;      // 사업비 산출내역 (JSON 형식)

    // AI 생성 내용
    @Column(columnDefinition = "TEXT")
    private String detailedPlan;       // 2. 세부계획

    @Column(columnDefinition = "TEXT")
    private String monthlyPlan;        // 3. 월별 추진계획

    @Column(columnDefinition = "TEXT")
    private String expectedEffect;     // 4. 기대효과

    // 메타 정보
    private String status;             // 작성중, 완료
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