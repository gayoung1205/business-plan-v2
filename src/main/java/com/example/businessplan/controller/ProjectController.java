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

    /**
     * 1단계: 사업개요 입력 + 질문 생성
     * POST /api/projects/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createProject(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("=== /create 요청 받음 ===");
            System.out.println("요청 데이터: " + requestData);

            // Project 객체 생성
            Project project = new Project();
            project.setCommunityName((String) requestData.get("communityName"));
            project.setProjectName((String) requestData.get("projectName"));
            project.setProjectPeriod((String) requestData.get("projectPeriod"));
            project.setProjectLocation((String) requestData.get("projectLocation"));

            // 숫자 변환 (안전하게)
            project.setTotalBudget(parseLong(requestData.get("totalBudget")));
            project.setProvincialFund(parseLong(requestData.get("provincialFund")));
            project.setCityFund(parseLong(requestData.get("cityFund")));
            project.setSelfFund(parseLong(requestData.get("selfFund")));

            // 엑셀 데이터가 있으면 저장
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

            // 프로젝트 생성 + 질문 생성
            Project savedProject = projectService.createProjectWithQuestions(project);

            System.out.println("프로젝트 생성 완료: " + savedProject.getId());

            // 생성된 질문 조회
            List<Question> questions = questionRepository.findByProjectIdOrderByOrderNum(savedProject.getId());

            System.out.println("질문 조회 완료: " + questions.size() + "개");

            // 응답
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

    // 헬퍼 메서드 (Controller 클래스 안에 추가)
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

    /**
     * 2단계: 답변 저장
     * POST /api/projects/answer
     */
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

    /**
     * 3단계: AI로 답변 확장
     * POST /api/projects/{projectId}/expand
     */
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

    /**
     * 4단계: 최종 사업계획서 생성
     * POST /api/projects/{projectId}/generate
     */
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

    /**
     * 프로젝트 조회
     * GET /api/projects/{projectId}
     */
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

    /**
     * 모든 프로젝트 조회
     * GET /api/projects
     */
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        return ResponseEntity.ok(projectRepository.findAll());
    }

    /**
     * 사업비 검증
     * POST /api/projects/validate-budget
     */
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

    /**
     * 엑셀 파일 업로드 & 파싱
     * POST /api/projects/upload-excel
     */
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

            // 파싱
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

    /**
     * DOCX 파일 다운로드
     * GET /api/projects/{projectId}/download
     */
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

    /**
     * 사업비 산출내역 엑셀 다운로드
     * GET /api/projects/{projectId}/download-budget
     */
    @GetMapping("/{projectId}/download-budget")
    public ResponseEntity<byte[]> downloadBudgetExcel(@PathVariable Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

            if (project.getBudgetDetails() == null || project.getBudgetDetails().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // JSON 파싱
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
}