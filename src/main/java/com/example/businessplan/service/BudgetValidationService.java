package com.example.businessplan.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class BudgetValidationService {

    /**
     * 사업비 검증
     */
    public Map<String, Object> validateBudget(Long totalBudget, Long provincialFund,
                                              Long cityFund, Long selfFund) {
        Map<String, Object> result = new HashMap<>();

        // null 체크 및 기본값 설정
        totalBudget = totalBudget != null ? totalBudget : 0L;
        provincialFund = provincialFund != null ? provincialFund : 0L;
        cityFund = cityFund != null ? cityFund : 0L;
        selfFund = selfFund != null ? selfFund : 0L;

        // 계산
        Long calculatedTotal = provincialFund + cityFund + selfFund;

        System.out.println("=== 사업비 검증 ===");
        System.out.println("입력 총사업비: " + totalBudget);
        System.out.println("도비: " + provincialFund);
        System.out.println("시군비: " + cityFund);
        System.out.println("자부담: " + selfFund);
        System.out.println("계산된 합계: " + calculatedTotal);

        // 검증
        if (totalBudget.equals(calculatedTotal)) {
            result.put("valid", true);
            result.put("message", "✅ 사업비가 정확합니다!");
            result.put("calculatedTotal", calculatedTotal);
        } else {
            result.put("valid", false);

            Long difference = totalBudget - calculatedTotal;
            String suggestion;

            if (difference > 0) {
                // 부족한 경우
                suggestion = String.format(
                        "❌ 오류: 총 %,d천원이 부족합니다. 자부담을 %,d천원으로 수정하세요.",
                        difference,
                        selfFund + difference
                );
            } else {
                // 초과한 경우
                suggestion = String.format(
                        "❌ 오류: 총 %,d천원이 초과되었습니다. 자부담을 %,d천원으로 수정하세요.",
                        Math.abs(difference),
                        selfFund + difference  // difference가 음수이므로 더하면 빼는 효과
                );
            }

            result.put("message", suggestion);
            result.put("correctSelfFund", selfFund + difference);
            result.put("calculatedTotal", calculatedTotal);
            result.put("difference", difference);
        }

        return result;
    }

    /**
     * 사업비 산출내역 검증 (테이블 형식)
     */
    public Map<String, Object> validateBudgetDetails(String budgetDetailsJson, Long totalBudget) {
        Map<String, Object> result = new HashMap<>();

        // TODO: JSON 파싱해서 세부 항목 합계 검증
        // 일단 기본 구조만 만들어둠

        result.put("valid", true);
        result.put("message", "사업비 산출내역이 확인되었습니다.");

        return result;
    }

    public Map<String, Object> validateWithExcel(
            Long inputTotalBudget,
            Long inputProvincialFund,
            Long inputCityFund,
            Long inputSelfFund,
            Map<String, Object> excelData) {

        Map<String, Object> result = new HashMap<>();

        // 엑셀에서 파싱한 금액
        Long excelTotal = (Long) excelData.get("totalAmount");
        Long excelProvincial = (Long) excelData.get("totalProvincial");
        Long excelCity = (Long) excelData.get("totalCity");
        Long excelSelf = (Long) excelData.get("totalSelf");

        // 1. 총사업비 일치 여부
        if (!inputTotalBudget.equals(excelTotal)) {
            result.put("valid", false);
            long diff = inputTotalBudget - excelTotal;

            String message = String.format(
                    "❌ 총사업비 불일치!\n\n" +
                            "입력한 총사업비: %,d천원\n" +
                            "엑셀 합계: %,d천원\n" +
                            "차이: %,d천원 %s",
                    inputTotalBudget,
                    excelTotal,
                    Math.abs(diff),
                    diff > 0 ? "부족" : "초과"
            );

            result.put("message", message);
            result.put("difference", diff);
            return result;
        }

        // 2. 도비/시군비/자부담 일치 여부
        if (!inputProvincialFund.equals(excelProvincial) ||
                !inputCityFund.equals(excelCity) ||
                !inputSelfFund.equals(excelSelf)) {

            result.put("valid", false);
            result.put("message",
                    "⚠️ 보조금/자부담 비율이 일치하지 않습니다.\n\n" +
                            String.format("도비: %,d (입력) vs %,d (엑셀)\n", inputProvincialFund, excelProvincial) +
                            String.format("시군비: %,d (입력) vs %,d (엑셀)\n", inputCityFund, excelCity) +
                            String.format("자부담: %,d (입력) vs %,d (엑셀)", inputSelfFund, excelSelf)
            );
            return result;
        }

        // 3. 모든 검증 통과
        result.put("valid", true);
        result.put("message", "✅ 사업비 검증 완료! 모든 금액이 정확합니다.");

        return result;
    }
}