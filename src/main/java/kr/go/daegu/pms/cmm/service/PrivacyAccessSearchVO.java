package kr.go.daegu.pms.cmm.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** 개인정보 처리 기록 검색 조건 (searchKeyword: 처리자 아이디) */
@Getter
@Setter
public class PrivacyAccessSearchVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    private String action;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /** 종료일 다음 날 (SQL 에서 accessed_at < 이 값 으로 비교) */
    public LocalDate getToDateExclusive() {
        return toDate == null ? null : toDate.plusDays(1);
    }
}
