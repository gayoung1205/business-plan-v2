package com.example.businessplan.service;

import com.example.businessplan.entity.Answer;
import com.example.businessplan.entity.Project;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptService {

    private final OpenAiService openAiService;
    private final String model;

    public GptService(@Value("${openai.api-key}") String apiKey,
                      @Value("${openai.model}") String model) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(60));
        this.model = model;
        System.out.println("✅ OpenAI 서비스 초기화 성공! (Model: " + model + ")");
    }

    /**
     * 세부계획 질문 생성 (2개)
     */
    public String generateDetailedPlanQuestions(String projectName, String projectLocation) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("다음 사업에 대해 '세부계획'을 작성하기 위한 질문 2개를 만들어주세요.\n\n");
            prompt.append("사업명: ").append(projectName).append("\n");
            prompt.append("위치: ").append(projectLocation).append("\n\n");
            prompt.append("질문 조건:\n");
            prompt.append("1. 존댓말 사용\n");
            prompt.append("2. 짧고 명확하게 (15~20자)\n");
            prompt.append("3. 세부사업 내용(일시/장소/참여인원/내용)을 알 수 있는 질문\n\n");
            prompt.append("출력 형식:\n");
            prompt.append("1. [질문]\n");
            prompt.append("2. [질문]\n");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 사업계획서 작성 전문가입니다."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(300)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            throw new RuntimeException("질문 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 월별 추진계획 질문 생성 (2개)
     */
    public String generateMonthlyPlanQuestions(String projectName, String projectPeriod) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("다음 사업의 '월별 추진계획'을 작성하기 위한 질문 2개를 만들어주세요.\n\n");
            prompt.append("사업명: ").append(projectName).append("\n");
            prompt.append("기간: ").append(projectPeriod).append("\n\n");
            prompt.append("질문 조건:\n");
            prompt.append("1. 존댓말 사용\n");
            prompt.append("2. 짧고 명확하게\n");
            prompt.append("3. 각 월별로 어떤 활동을 할 계획인지 알 수 있는 질문\n\n");
            prompt.append("출력 형식:\n");
            prompt.append("1. [질문]\n");
            prompt.append("2. [질문]\n");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 사업계획서 작성 전문가입니다."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(300)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            throw new RuntimeException("질문 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기대효과 질문 생성 (2개)
     */
    public String generateExpectedEffectQuestions(String projectName) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("다음 사업의 '기대효과'를 작성하기 위한 질문 2개를 만들어주세요.\n\n");
            prompt.append("사업명: ").append(projectName).append("\n\n");
            prompt.append("질문 조건:\n");
            prompt.append("1. 존댓말 사용\n");
            prompt.append("2. 짧고 명확하게\n");
            prompt.append("3. 사업의 긍정적 영향이나 기대효과를 알 수 있는 질문\n\n");
            prompt.append("출력 형식:\n");
            prompt.append("1. [질문]\n");
            prompt.append("2. [질문]\n");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 사업계획서 작성 전문가입니다."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(300)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            throw new RuntimeException("질문 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 답변을 전문적인 문장으로 확장
     */
    public String expandAnswer(String question, String userAnswer, String section) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("다음 질문에 대한 사용자의 답변을 사업계획서에 적합한 전문적인 문장으로 확장해주세요.\n\n");
            prompt.append("섹션: ").append(section).append("\n");
            prompt.append("질문: ").append(question).append("\n");
            prompt.append("답변: ").append(userAnswer).append("\n\n");
            prompt.append("조건:\n");
            prompt.append("1. 격식있고 전문적인 문체로\n");
            prompt.append("2. 3~5문장으로 확장\n");
            prompt.append("3. 구체적이고 설득력있게\n");
            prompt.append("4. 평가자가 이해하기 쉽게\n");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system", "당신은 정부 지원사업 사업계획서 작성 전문가입니다."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            return openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

        } catch (Exception e) {
            throw new RuntimeException("답변 확장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 질문/답변 기반으로 2페이지 내용 생성 (세부계획, 월별계획, 기대효과)
     */
    public Map<String, String> generatePage2Content(
            Project project,
            List<Answer> answers) {

        try {
            // 답변을 섹션별로 정리
            StringBuilder detailedAnswers = new StringBuilder();
            StringBuilder monthlyAnswers = new StringBuilder();
            StringBuilder effectAnswers = new StringBuilder();

            for (Answer answer : answers) {
                String section = answer.getQuestion().getSection();
                String questionText = answer.getQuestion().getQuestionText();
                String userAnswer = answer.getUserAnswer();

                String line = "Q: " + questionText + "\nA: " + userAnswer + "\n\n";

                if (section.contains("세부계획")) {
                    detailedAnswers.append(line);
                } else if (section.contains("월별")) {
                    monthlyAnswers.append(line);
                } else if (section.contains("기대효과")) {
                    effectAnswers.append(line);
                }
            }

            StringBuilder prompt = new StringBuilder();

            prompt.append("=== 사업 기본 정보 ===\n");
            prompt.append("공동체명: ").append(project.getCommunityName()).append("\n");
            prompt.append("사업명: ").append(project.getProjectName()).append("\n");
            prompt.append("기간: ").append(project.getProjectPeriod()).append("\n");
            prompt.append("위치: ").append(project.getProjectLocation()).append("\n\n");

            prompt.append("=== 세부계획 관련 답변 ===\n");
            prompt.append(detailedAnswers.toString()).append("\n");

            prompt.append("=== 월별계획 관련 답변 ===\n");
            prompt.append(monthlyAnswers.toString()).append("\n");

            prompt.append("=== 기대효과 관련 답변 ===\n");
            prompt.append(effectAnswers.toString()).append("\n");

            prompt.append("위 정보를 바탕으로 사업 실행계획서를 작성해주세요.\n\n");

            prompt.append("【2. 세부계획】\n");
            prompt.append("가) 세부사업별 내용\n");
            prompt.append("❍ (세부 사업명)\n");
            prompt.append("- 일시/장소: (구체적으로 작성)\n");
            prompt.append("- 참여인원: (명확한 인원수)\n");
            prompt.append("- 사업내용: (상세하게 2-3문장으로)\n\n");

            prompt.append("필요시 여러 세부사업 작성\n\n");

            prompt.append("【3. 월별 추진계획】\n");
            prompt.append("사업기간에 맞춰 각 월별로 추진할 내용을 구체적으로 작성\n");
            prompt.append("형식:\n");
            prompt.append("3월\n❍ 세부내용\n- 구체적 활동\n\n");
            prompt.append("4월\n❍ 세부내용\n- 구체적 활동\n\n");
            prompt.append("(사업기간 동안 계속)\n\n");

            prompt.append("【4. 기대효과】\n");
            prompt.append("❍ (효과 1 - 2-3문장)\n");
            prompt.append("❍ (효과 2 - 2-3문장)\n\n");

            prompt.append("작성 원칙:\n");
            prompt.append("- 격식있고 전문적인 문체 사용\n");
            prompt.append("- 신청자 답변의 의도를 정확히 반영\n");
            prompt.append("- 구체적이고 실현 가능한 내용으로\n");
            prompt.append("- 평가자가 납득할 수 있는 설득력 있는 내용\n");
            prompt.append("- 각 섹션은 명확히 구분하여 작성\n\n");

            prompt.append("출력 형식:\n");
            prompt.append("[세부계획]\n내용...\n\n");
            prompt.append("[월별추진계획]\n내용...\n\n");
            prompt.append("[기대효과]\n내용...\n");

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage("system",
                    "당신은 정부 지원사업 사업계획서 작성 전문가입니다. " +
                            "신청자의 답변을 바탕으로 전문적이고 설득력있는 계획서를 작성합니다. " +
                            "형식을 정확히 지키고, 각 섹션을 명확히 구분하여 작성합니다."));
            messages.add(new ChatMessage("user", prompt.toString()));

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build();

            String response = openAiService.createChatCompletion(request)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            System.out.println("=== GPT 응답 ===");
            System.out.println(response);
            System.out.println("=== 응답 끝 ===");

            // 응답 파싱
            return parsePageContent(response);

        } catch (Exception e) {
            throw new RuntimeException("페이지 내용 생성 실패: " + e.getMessage(), e);
        }
    }

    /**
     * GPT 응답을 섹션별로 파싱
     */
    private Map<String, String> parsePageContent(String response) {
        Map<String, String> sections = new LinkedHashMap<>();

        String[] lines = response.split("\n");
        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            // [섹션명] 패턴 찾기
            if (line.trim().startsWith("[") && line.trim().endsWith("]")) {
                // 이전 섹션 저장
                if (currentSection != null && currentContent.length() > 0) {
                    sections.put(currentSection, currentContent.toString().trim());
                }

                // 새 섹션 시작
                currentSection = line.trim().substring(1, line.trim().length() - 1);
                currentContent = new StringBuilder();
            } else if (currentSection != null) {
                // 현재 섹션 내용 추가
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(line);
            }
        }

        // 마지막 섹션 저장
        if (currentSection != null && currentContent.length() > 0) {
            sections.put(currentSection, currentContent.toString().trim());
        }

        System.out.println("=== 파싱 결과 ===");
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            System.out.println("섹션: " + entry.getKey());
            System.out.println("내용 길이: " + entry.getValue().length());
        }

        return sections;
    }
}