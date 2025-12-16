package com.example.businessplan.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "answers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Question question;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;         // 사용자 답변

    @Column(columnDefinition = "TEXT")
    private String aiGeneratedText;    // AI가 확장한 전문 문장
}