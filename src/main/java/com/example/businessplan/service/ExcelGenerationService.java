package com.example.businessplan.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExcelGenerationService {

    /**
     * 사업비 산출내역을 엑셀 파일로 생성
     */
    public byte[] generateBudgetExcel(Map<String, Object> budgetData) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("사업비 산출내역");

        // 스타일 설정
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        // 헤더 생성
        Row headerRow = sheet.createRow(0);
        String[] headers = {"세부사업", "사업비목", "산출근거", "계", "도비(30%)", "시군비(70%)", "자부담"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터 입력
        List<Map<String, Object>> items = (List<Map<String, Object>>) budgetData.get("items");
        int rowNum = 1;

        for (Map<String, Object> item : items) {
            Row row = sheet.createRow(rowNum++);

            createCell(row, 0, getString(item.get("subProject")), dataStyle);
            createCell(row, 1, getString(item.get("budgetItem")), dataStyle);
            createCell(row, 2, getString(item.get("calculation")), dataStyle);
            createCell(row, 3, getLong(item.get("amount")), numberStyle);
            createCell(row, 4, getLong(item.get("provincialFund")), numberStyle);
            createCell(row, 5, getLong(item.get("cityFund")), numberStyle);
            createCell(row, 6, getLong(item.get("selfFund")), numberStyle);
        }

        // 합계 행
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("합계");
        totalLabelCell.setCellStyle(headerStyle);

        Cell emptyCell1 = totalRow.createCell(1);
        emptyCell1.setCellStyle(headerStyle);
        Cell emptyCell2 = totalRow.createCell(2);
        emptyCell2.setCellStyle(headerStyle);

        createCell(totalRow, 3, getLong(budgetData.get("totalAmount")), headerStyle);
        createCell(totalRow, 4, getLong(budgetData.get("totalProvincial")), headerStyle);
        createCell(totalRow, 5, getLong(budgetData.get("totalCity")), headerStyle);
        createCell(totalRow, 6, getLong(budgetData.get("totalSelf")), headerStyle);

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }

        // ByteArray로 변환
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private String getString(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    private long getLong(Object value) {
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
}