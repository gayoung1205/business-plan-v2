package com.example.businessplan.controller;

import com.example.businessplan.entity.Project;
import com.example.businessplan.entity.Question;
import com.example.businessplan.entity.Answer;
import com.example.businessplan.repository.ProjectRepository;
import com.example.businessplan.repository.QuestionRepository;
import com.example.businessplan.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final QuestionRepository questionRepository;
    private final BudgetValidationService budgetValidationService;
    private final ExcelParsingService excelParsingService;
    private final DocumentGenerationService documentGenerationService;
    private final ExcelGenerationService excelGenerationService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== /create 요청 받음 ===");
            System.out.println("요청 데이터: " + requestData);

            Project project = new Project();
            project.setCommunityName((String) requestData.get("communityName"));
            project.setProjectName((String) requestData.get("projectName"));
            project.setProjectPeriod((String) requestData.get("projectPeriod"));
            project.setProjectLocation((String) requestData.get("projectLocation"));

            project.setTotalBudget(parseLong(requestData.get("totalBudget")));
            project.setProvincialFund(parseLong(requestData.get("provincialFund")));
            project.setCityFund(parseLong(requestData.get("cityFund")));
            project.setSelfFund(parseLong(requestData.get("selfFund")));

            if (requestData.containsKey("excelData") && requestData.get("excelData") != null) {
                try {
                    String budgetDetails = new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(requestData.get("excelData"));
                    project.setBudgetDetails(budgetDetails);
                    System.out.println("엑셀 데이터 저장됨");
                } catch (Exception e) {
                    System.err.println("엑셀 데이터 변환 실패: " + e.getMessage());
                }
            }

            System.out.println("프로젝트 생성 시작...");

            Project savedProject = projectService.createProjectWithQuestions(project);

            System.out.println("프로젝트 생성 완료: " + savedProject.getId());

            List<Question> questions = questionRepository.findByProjectIdOrderByOrderNum(savedProject.getId());

            System.out.println("질문 조회 완료: " + questions.size() + "개");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "사업개요 저장 및 질문 생성 완료!");
            response.put("project", savedProject);
            response.put("questions", questions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("=== 에러 발생! ===");
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "프로젝트 생성 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<Map<String, Object>> saveAnswer(@RequestBody Map<String, Object> request) {
        try {
            Long questionId = Long.parseLong(request.get("questionId").toString());
            String userAnswer = (String) request.get("userAnswer");

            Answer savedAnswer = projectService.saveAnswer(questionId, userAnswer);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "답변 저장 완료!");
            response.put("answer", savedAnswer);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "답변 저장 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{projectId}/expand")
    public ResponseEntity<Map<String, Object>> expandAnswers(@PathVariable Long projectId) {
        try {
            projectService.expandAllAnswers(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "답변 확장 완료!");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "답변 확장 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{projectId}/generate")
    public ResponseEntity<Map<String, Object>> generateFinalPlan(@PathVariable Long projectId) {
        try {
            Project project = projectService.generateFinalPlan(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "사업계획서 생성 완료!");
            response.put("project", project);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "사업계획서 생성 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<Map<String, Object>> getProject(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

            List<Question> questions = questionRepository.findByProjectIdOrderByOrderNum(projectId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("project", project);
            response.put("questions", questions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @PostMapping("/validate-budget")
    public ResponseEntity<Map<String, Object>> validateBudget(@RequestBody Map<String, Long> budget) {
        try {
            Long totalBudget = budget.get("totalBudget");
            Long provincialFund = budget.get("provincialFund");
            Long cityFund = budget.get("cityFund");
            Long selfFund = budget.get("selfFund");

            Map<String, Object> result = budgetValidationService.validateBudget(
                    totalBudget, provincialFund, cityFund, selfFund);

            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "검증 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/upload-excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== 엑셀 파일 업로드 ===");
            System.out.println("파일명: " + file.getOriginalFilename());
            System.out.println("크기: " + file.getSize() + " bytes");

            // 파일 검증
            if (file.isEmpty()) {
                throw new RuntimeException("파일이 비어있습니다");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                throw new RuntimeException("엑셀 파일(.xlsx, .xls)만 업로드 가능합니다");
            }

            Map<String, Object> parseResult = excelParsingService.parseExcel(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "엑셀 파일 파싱 완료!");
            response.put("data", parseResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("엑셀 업로드 실패: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{projectId}/download")
    public ResponseEntity<byte[]> downloadDocx(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

            byte[] docxBytes = documentGenerationService.generateDocx(project);

            String filename = project.getProjectName() + "_사업계획서.docx";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" +
                            new String(filename.getBytes("UTF-8"), "ISO-8859-1") + "\"")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .body(docxBytes);

        } catch (Exception e) {
            System.err.println("파일 생성 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/download-budget")
    public ResponseEntity<byte[]> downloadBudgetExcel(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

            if (project.getBudgetDetails() == null || project.getBudgetDetails().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> budgetData = mapper.readValue(
                    project.getBudgetDetails(),
                    new TypeReference<Map<String, Object>>() {}
            );

            byte[] excelBytes = excelGenerationService.generateBudgetExcel(budgetData);

            String filename = project.getProjectName() + "_사업비산출내역.xlsx";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" +
                            new String(filename.getBytes("UTF-8"), "ISO-8859-1") + "\"")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(excelBytes);

        } catch (Exception e) {
            System.err.println("엑셀 생성 실패: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save-draft")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== /save-draft 요청 받음 ===");

            Project project = new Project();
            project.setCommunityName((String) requestData.get("communityName"));
            project.setProjectName((String) requestData.get("projectName"));
            project.setProjectPeriod((String) requestData.get("projectPeriod"));
            project.setProjectLocation((String) requestData.get("projectLocation"));

            project.setTotalBudget(parseLong(requestData.get("totalBudget")));
            project.setProvincialFund(parseLong(requestData.get("provincialFund")));
            project.setCityFund(parseLong(requestData.get("cityFund")));
            project.setSelfFund(parseLong(requestData.get("selfFund")));

            if (requestData.containsKey("excelData") && requestData.get("excelData") != null) {
                try {
                    String budgetDetails = new ObjectMapper()
                            .writeValueAsString(requestData.get("excelData"));
                    project.setBudgetDetails(budgetDetails);
                } catch (Exception e) {
                    System.err.println("엑셀 데이터 변환 실패: " + e.getMessage());
                }
            }

            project.setStatus("임시저장");

            Project savedProject = projectRepository.save(project);

            System.out.println("임시저장 완료: " + savedProject.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "사업개요가 저장되었습니다");
            response.put("projectId", savedProject.getId());
            response.put("project", savedProject);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("임시저장 실패: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "임시저장 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{projectId}/generate-questions")
    public ResponseEntity<Map<String, Object>> generateQuestions(@PathVariable Long projectId) {
        try {
            System.out.println("=== /generate-questions 요청 받음 ===");
            System.out.println("프로젝트 ID: " + projectId);

            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

            List<Question> existingQuestions = questionRepository.findByProjectIdOrderByOrderNum(projectId);
            if (!existingQuestions.isEmpty()) {
                System.out.println("이미 질문이 존재함");

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "질문이 이미 생성되어 있습니다");
                response.put("projectId", project.getId());
                response.put("project", project);
                response.put("questions", existingQuestions);

                return ResponseEntity.ok(response);
            }

            System.out.println("질문 생성 시작...");
            projectService.generateQuestionsForProject(project);

            List<Question> questions = questionRepository.findByProjectIdOrderByOrderNum(projectId);
            System.out.println("질문 생성 완료: " + questions.size() + "개");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "질문 생성 완료!");
            response.put("projectId", project.getId());
            response.put("project", project);
            response.put("questions", questions);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("질문 생성 실패: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "질문 생성 실패: " + e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/auto-adjust-budget")
    public ResponseEntity<Map<String, Object>> autoAdjustBudget(@RequestBody Map<String, Object> request) {
        try {
            Long targetTotal = Long.parseLong(request.get("targetTotal").toString());
            List<Map<String, Object>> itemsRaw = (List<Map<String, Object>>) request.get("items");

            // items 복사
            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> item : itemsRaw) {
                items.add(new HashMap<>(item));
            }

            // 현재 합계 계산
            Long currentTotal = items.stream()
                    .mapToLong(item -> ((Number) item.get("amount")).longValue())
                    .sum();

            if (!currentTotal.equals(targetTotal)) {
                Long difference = currentTotal - targetTotal;

                // 마지막 항목 조정
                Map<String, Object> lastItem = items.get(items.size() - 1);
                Long oldAmount = ((Number) lastItem.get("amount")).longValue();
                Long newAmount = oldAmount - difference;

                // ✨ 산출근거 재계산
                String oldCalculation = (String) lastItem.get("calculation");
                String newCalculation = recalculateCalculation(oldCalculation, newAmount);

                lastItem.put("amount", newAmount);
                lastItem.put("calculation", newCalculation);

                // 도비/시군비도 재계산
                Long newProvincial = Math.round(newAmount * 0.3);
                Long newCity = Math.round(newAmount * 0.7);
                lastItem.put("provincialFund", newProvincial);
                lastItem.put("cityFund", newCity);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", items);
            response.put("message", "자동 조정 완료");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.ok(errorResponse);
        }
    }

    private String recalculateCalculation(String originalCalculation, Long newAmount) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)원?\\s*[×xX*]\\s*(\\d+)([^\\d]*)");
        java.util.regex.Matcher matcher = pattern.matcher(originalCalculation);

        if (matcher.find()) {
            try {
                long unitPrice = Long.parseLong(matcher.group(1));
                String unit = matcher.group(3).trim();

                long newQuantity = Math.round((double) newAmount / unitPrice);

                return String.format("%d원 × %d%s", unitPrice, newQuantity, unit);
            } catch (NumberFormatException e) {
                return originalCalculation + " (조정됨)";
            }
        }

        return originalCalculation + " (조정됨)";
    }
}