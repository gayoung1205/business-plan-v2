package com.example.businessplan.service;

import com.example.businessplan.entity.Project;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class DocumentGenerationService {

    public byte[] generateDocx(Project project) throws IOException {
        XWPFDocument document = new XWPFDocument();

        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("사업 실행계획서");
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setFontFamily("맑은 고딕");
        addEmptyLine(document);

        addSectionTitle(document, "1. 사업개요");
        addTableRow(document, "공동체명", project.getCommunityName());
        addTableRow(document, "사업명", project.getProjectName());
        addTableRow(document, "사업기간", project.getProjectPeriod());
        addTableRow(document, "사업위치", project.getProjectLocation());

        String budgetInfo = String.format(
                "총 %,d천원 (도비 %,d, 시군비 %,d, 자부담 %,d)",
                project.getTotalBudget(),
                project.getProvincialFund(),
                project.getCityFund(),
                project.getSelfFund()
        );
        addTableRow(document, "사업비", budgetInfo);
        addEmptyLine(document);

        if (project.getBudgetDetails() != null && !project.getBudgetDetails().isEmpty()) {
            try {
                addSectionTitle(document, "사업비 산출내역");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> budgetData = mapper.readValue(
                        project.getBudgetDetails(),
                        new TypeReference<Map<String, Object>>() {}
                );
                addBudgetTable(document, budgetData);
                addEmptyLine(document);
            } catch (Exception e) {
                System.err.println("사업비 표 추가 실패: " + e.getMessage());
            }
        }

        if (project.getDetailedPlan() != null) {
            addSectionTitle(document, "2. 세부계획");
            addContent(document, project.getDetailedPlan());
            addEmptyLine(document);
        }

        if (project.getMonthlyPlan() != null) {
            addSectionTitle(document, "3. 월별 추진계획");
            addContent(document, project.getMonthlyPlan());
            addEmptyLine(document);
        }

        if (project.getExpectedEffect() != null) {
            addSectionTitle(document, "4. 기대효과");
            addContent(document, project.getExpectedEffect());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        return out.toByteArray();
    }

    private void addBudgetTable(XWPFDocument document, Map<String, Object> budgetData) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) budgetData.get("items");

        XWPFTable table = document.createTable(items.size() + 2, 7);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        String[] headers = {"세부사업", "사업비목", "산출근거", "계", "도비(30%)", "시군비(70%)", "자부담"};

        for (int i = 0; i < headers.length; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            cell.setColor("F8F9FA");
            XWPFParagraph para = cell.getParagraphs().get(0);
            para.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = para.createRun();
            run.setText(headers[i]);
            run.setBold(true);
            run.setFontSize(10);
            run.setFontFamily("맑은 고딕");
        }

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            XWPFTableRow row = table.getRow(i + 1);

            setCellText(row.getCell(0), (String) item.get("subProject"), false);
            setCellText(row.getCell(1), (String) item.get("budgetItem"), false);
            setCellText(row.getCell(2), (String) item.get("calculation"), false);
            setCellText(row.getCell(3), String.format("%,d", getLong(item.get("amount"))), true);
            setCellText(row.getCell(4), String.format("%,d", getLong(item.get("provincialFund"))), true);
            setCellText(row.getCell(5), String.format("%,d", getLong(item.get("cityFund"))), true);
            setCellText(row.getCell(6), String.format("%,d", getLong(item.get("selfFund"))), true);
        }

        XWPFTableRow totalRow = table.getRow(items.size() + 1);
        XWPFTableCell totalCell = totalRow.getCell(0);
        totalCell.setColor("F8F9FA");
        setCellText(totalCell, "합계", false, true);

        totalRow.getCell(1).setColor("F8F9FA");
        totalRow.getCell(2).setColor("F8F9FA");

        setCellText(totalRow.getCell(3), String.format("%,d", budgetData.get("totalAmount")), true, true);
        setCellText(totalRow.getCell(4), String.format("%,d", budgetData.get("totalProvincial")), true, true);
        setCellText(totalRow.getCell(5), String.format("%,d", budgetData.get("totalCity")), true, true);
        setCellText(totalRow.getCell(6), String.format("%,d", budgetData.get("totalSelf")), true, true);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean rightAlign) {
        setCellText(cell, text, rightAlign, false);
    }

    private void setCellText(XWPFTableCell cell, String text, boolean rightAlign, boolean bold) {
        XWPFParagraph para = cell.getParagraphs().get(0);
        if (rightAlign) {
            para.setAlignment(ParagraphAlignment.RIGHT);
        }
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(10);
        run.setFontFamily("맑은 고딕");
        if (bold) {
            run.setBold(true);
        }
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

    private void addSectionTitle(XWPFDocument document, String title) {
        XWPFParagraph para = document.createParagraph();
        XWPFRun run = para.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("맑은 고딕");
    }

    private void addContent(XWPFDocument document, String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            XWPFParagraph para = document.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(line);
            run.setFontSize(11);
            run.setFontFamily("맑은 고딕");
        }
    }

    private void addTableRow(XWPFDocument document, String label, String value) {
        XWPFTable table = document.createTable(1, 2);
        table.setWidth("100%");

        XWPFTableRow row = table.getRow(0);

        XWPFTableCell cell1 = row.getCell(0);
        cell1.setColor("F8F9FA");
        cell1.setWidth("30%");
        XWPFParagraph para1 = cell1.getParagraphs().get(0);
        XWPFRun run1 = para1.createRun();
        run1.setText(label);
        run1.setBold(true);
        run1.setFontSize(11);
        run1.setFontFamily("맑은 고딕");

        XWPFTableCell cell2 = row.getCell(1);
        cell2.setWidth("70%");
        XWPFParagraph para2 = cell2.getParagraphs().get(0);
        XWPFRun run2 = para2.createRun();
        run2.setText(value);
        run2.setFontSize(11);
        run2.setFontFamily("맑은 고딕");
    }

    private void addEmptyLine(XWPFDocument document) {
        XWPFParagraph para = document.createParagraph();
        para.createRun().addBreak();
    }
}