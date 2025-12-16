package com.example.businessplan.service;

import com.example.businessplan.entity.Project;
import com.example.businessplan.entity.Question;
import com.example.businessplan.entity.Answer;
import com.example.businessplan.repository.ProjectRepository;
import com.example.businessplan.repository.QuestionRepository;
import com.example.businessplan.repository.AnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final GptService gptService;
    private final BudgetValidationService budgetValidationService;

    /**
     * 1단계: 사업개요 저장 + 질문 생성
     */
    @Transactional
    public Project createProjectWithQuestions(Project project) {

        // 1. 사업개요 저장
        project.setStatus("질문생성중");
        Project savedProject = projectRepository.save(project);

        // 2. 질문 생성
        String detailedQuestions = gptService.generateDetailedPlanQuestions(
                project.getProjectName(), project.getProjectLocation());

        String monthlyQuestions = gptService.generateMonthlyPlanQuestions(
                project.getProjectName(), project.getProjectPeriod());

        String effectQuestions = gptService.generateExpectedEffectQuestions(
                project.getProjectName());

        // 3. 질문 파싱 및 저장
        saveQuestions(savedProject, "세부계획", detailedQuestions);
        saveQuestions(savedProject, "월별추진계획", monthlyQuestions);
        saveQuestions(savedProject, "기대효과", effectQuestions);

        // 4. 상태 업데이트
        savedProject.setStatus("질문답변대기");
        projectRepository.save(savedProject);

        return savedProject;
    }

    /**
     * 질문 파싱 및 저장
     */
    private void saveQuestions(Project project, String section, String questionsText) {
        Pattern pattern = Pattern.compile("(\\d+)\\.\\s*(.+)");
        Matcher matcher = pattern.matcher(questionsText);

        int order = 1;
        while (matcher.find()) {
            String questionText = matcher.group(2).trim();

            Question question = new Question();
            question.setProject(project);
            question.setSection(section);
            question.setQuestionText(questionText);
            question.setOrderNum(order++);

            questionRepository.save(question);
        }
    }

    /**
     * 2단계: 답변 저장
     */
    @Transactional
    public Answer saveAnswer(Long questionId, String userAnswer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문을 찾을 수 없습니다"));

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setUserAnswer(userAnswer);

        return answerRepository.save(answer);
    }

    /**
     * 3단계: 답변 확장 (AI)
     */
    @Transactional
    public void expandAllAnswers(Long projectId) {
        List<Answer> answers = answerRepository.findByQuestionProjectId(projectId);

        for (Answer answer : answers) {
            if (answer.getAiGeneratedText() == null) {
                String expanded = gptService.expandAnswer(
                        answer.getQuestion().getQuestionText(),
                        answer.getUserAnswer(),
                        answer.getQuestion().getSection()
                );

                answer.setAiGeneratedText(expanded);
                answerRepository.save(answer);
            }
        }
    }

    /**
     * 4단계: 최종 사업계획서 생성
     */
    @Transactional
    public Project generateFinalPlan(Long projectId) {
        System.out.println("=== 최종 계획서 생성 시작 ===");
        System.out.println("프로젝트 ID: " + projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

        List<Answer> answers = answerRepository.findByQuestionProjectId(projectId);

        System.out.println("답변 개수: " + answers.size());

        // GPT로 2페이지 내용 생성 (세부계획, 월별계획, 기대효과)
        Map<String, String> page2Content = gptService.generatePage2Content(project, answers);

        // Project에 저장
        project.setDetailedPlan(page2Content.get("세부계획"));
        project.setMonthlyPlan(page2Content.get("월별추진계획"));
        project.setExpectedEffect(page2Content.get("기대효과"));
        project.setStatus("완료");

        System.out.println("=== 생성 완료 ===");
        System.out.println("세부계획 길이: " + (project.getDetailedPlan() != null ? project.getDetailedPlan().length() : 0));
        System.out.println("월별계획 길이: " + (project.getMonthlyPlan() != null ? project.getMonthlyPlan().length() : 0));
        System.out.println("기대효과 길이: " + (project.getExpectedEffect() != null ? project.getExpectedEffect().length() : 0));

        return projectRepository.save(project);
    }
}
