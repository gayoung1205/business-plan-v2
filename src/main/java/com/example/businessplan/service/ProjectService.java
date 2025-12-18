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

    @Transactional
    public Project createProjectWithQuestions(Project project) {

        project.setStatus("질문생성중");
        Project savedProject = projectRepository.save(project);

        String detailedQuestions = gptService.generateDetailedPlanQuestions(
                project.getProjectName(), project.getProjectLocation());

        String monthlyQuestions = gptService.generateMonthlyPlanQuestions(
                project.getProjectName(), project.getProjectPeriod());

        String effectQuestions = gptService.generateExpectedEffectQuestions(
                project.getProjectName());

        saveQuestions(savedProject, "세부계획", detailedQuestions);
        saveQuestions(savedProject, "월별추진계획", monthlyQuestions);
        saveQuestions(savedProject, "기대효과", effectQuestions);

        savedProject.setStatus("질문답변대기");
        projectRepository.save(savedProject);

        return savedProject;
    }

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

    @Transactional
    public Answer saveAnswer(Long questionId, String userAnswer) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("질문을 찾을 수 없습니다"));

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setUserAnswer(userAnswer);

        return answerRepository.save(answer);
    }

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

    @Transactional
    public Project generateFinalPlan(Long projectId) {
        System.out.println("=== 최종 계획서 생성 시작 ===");
        System.out.println("프로젝트 ID: " + projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

        List<Answer> answers = answerRepository.findByQuestionProjectId(projectId);

        System.out.println("답변 개수: " + answers.size());

        Map<String, String> page2Content = gptService.generatePage2Content(project, answers);

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

    @Transactional
    public void generateQuestionsForProject(Project project) {
        System.out.println("=== 질문 생성 시작 (프로젝트 ID: " + project.getId() + ") ===");

        project.setStatus("질문생성중");
        projectRepository.save(project);

        String detailedQuestions = gptService.generateDetailedPlanQuestions(
                project.getProjectName(), project.getProjectLocation());

        String monthlyQuestions = gptService.generateMonthlyPlanQuestions(
                project.getProjectName(), project.getProjectPeriod());

        String effectQuestions = gptService.generateExpectedEffectQuestions(
                project.getProjectName());

        saveQuestions(project, "세부계획", detailedQuestions);
        saveQuestions(project, "월별추진계획", monthlyQuestions);
        saveQuestions(project, "기대효과", effectQuestions);

        project.setStatus("질문답변대기");
        projectRepository.save(project);

        System.out.println("=== 질문 생성 완료 ===");
    }
}
