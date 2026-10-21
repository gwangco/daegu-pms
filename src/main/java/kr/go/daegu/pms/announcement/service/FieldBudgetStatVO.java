package kr.go.daegu.pms.announcement.service;

import lombok.Data;

/** 메인 대시보드 차트용: 전략산업 분야별 공고 예산 합계 */
@Data
public class FieldBudgetStatVO {
    private String fieldCode;
    private String fieldName;
    private long totalBudget;
}
