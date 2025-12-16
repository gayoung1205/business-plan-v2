package com.example.businessplan.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ExcelParsingService {

    /**
     * 엑셀 파일에서 사업비 산출내역 파싱
     */
    public Map<String, Object> parseExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 헤더 찾기 (첫 번째 행 또는 "세부사업" 포함 행)
            int headerRow = findHeaderRow(sheet);

            if (headerRow == -1) {
                throw new RuntimeException("엑셀 양식이 올바르지 않습니다. '세부사업' 헤더를 찾을 수 없습니다.");
            }

            // 데이터 읽기 (헤더 다음 행부터)
            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> item = parseRow(row);
                if (item != null && !item.isEmpty()) {
                    items.add(item);
                }
            }

            // 합계 계산
            long totalAmount = items.stream()
                    .mapToLong(item -> (Long) item.getOrDefault("amount", 0L))
                    .sum();

            long totalProvincial = items.stream()
                    .mapToLong(item -> (Long) item.getOrDefault("provincialFund", 0L))
                    .sum();

            long totalCity = items.stream()
                    .mapToLong(item -> (Long) item.getOrDefault("cityFund", 0L))
                    .sum();

            long totalSelf = items.stream()
                    .mapToLong(item -> (Long) item.getOrDefault("selfFund", 0L))
                    .sum();

            result.put("items", items);
            result.put("totalAmount", totalAmount);
            result.put("totalProvincial", totalProvincial);
            result.put("totalCity", totalCity);
            result.put("totalSelf", totalSelf);
            result.put("itemCount", items.size());

            return result;

        } catch (Exception e) {
            throw new RuntimeException("엑셀 파일 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 헤더 행 찾기
     */
    private int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (Cell cell : row) {
                String value = getCellValueAsString(cell);
                if (value.contains("세부사업") || value.contains("사업비목")) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 행 데이터 파싱
     */
    private Map<String, Object> parseRow(Row row) {
        Map<String, Object> item = new HashMap<>();

        try {
            // 세부사업 (0번 열)
            String subProject = getCellValueAsString(row.getCell(0));
            if (subProject.isEmpty() || subProject.equals("소계") || subProject.equals("합계")) {
                return null; // 소계/합계 행은 건너뛰기
            }

            // 사업비목 (1번 열)
            String budgetItem = getCellValueAsString(row.getCell(1));

            // 산출근거 (2번 열)
            String calculation = getCellValueAsString(row.getCell(2));

            // 계 (3번 열)
            long amount = getCellValueAsLong(row.getCell(3));

            // 도비 (4번 열)
            long provincialFund = getCellValueAsLong(row.getCell(4));

            // 시군비 (5번 열)
            long cityFund = getCellValueAsLong(row.getCell(5));

            // 자부담 (6번 열)
            long selfFund = getCellValueAsLong(row.getCell(6));

            item.put("subProject", subProject);
            item.put("budgetItem", budgetItem);
            item.put("calculation", calculation);
            item.put("amount", amount);
            item.put("provincialFund", provincialFund);
            item.put("cityFund", cityFund);
            item.put("selfFund", selfFund);

            return item;

        } catch (Exception e) {
            System.err.println("행 파싱 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 셀 값을 문자열로 변환
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    /**
     * 셀 값을 숫자(Long)로 변환
     */
    private long getCellValueAsLong(Cell cell) {
        if (cell == null) return 0L;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (long) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim()
                            .replaceAll("[^0-9]", ""); // 숫자만 추출
                    return value.isEmpty() ? 0L : Long.parseLong(value);
                case FORMULA:
                    return (long) cell.getNumericCellValue();
                default:
                    return 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }
}