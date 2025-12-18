package com.example.businessplan.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ExcelParsingService {

    public Map<String, Object> parseExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            int headerRow = findHeaderRow(sheet);

            if (headerRow == -1) {
                throw new RuntimeException("엑셀 양식이 올바르지 않습니다. '세부사업' 헤더를 찾을 수 없습니다.");
            }

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> item = parseRow(row);
                if (item != null && !item.isEmpty()) {
                    items.add(item);
                }
            }

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

    private Map<String, Object> parseRow(Row row) {
        Map<String, Object> item = new HashMap<>();

        try {
            String subProject = getCellValueAsString(row.getCell(0));
            if (subProject.isEmpty() || subProject.equals("소계") || subProject.equals("합계")) {
                return null;
            }

            String budgetItem = getCellValueAsString(row.getCell(1));
            String calculation = getCellValueAsString(row.getCell(2));
            long amount = getCellValueAsLong(row.getCell(3));
            long provincialFund = getCellValueAsLong(row.getCell(4));
            long cityFund = getCellValueAsLong(row.getCell(5));
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

    private long getCellValueAsLong(Cell cell) {
        if (cell == null) return 0L;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (long) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim()
                            .replaceAll("[^0-9]", "");
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