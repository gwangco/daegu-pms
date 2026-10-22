package kr.go.daegu.pms.faq.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 자주 묻는 질문 VO.
 *
 * eGovFrame VS Code 확장의 CRUD 코드 생성기(DDL: docs/egov-codegen/faq.ddl.sql)로 만든 FaqVO 를 바탕으로 수정했다.
 * 공지사항(NoticeVO)과 같은 기준으로 보완: 공통 ComDefaultVO 상속, LocalDateTime, Bean Validation, Lombok.
 * 목록 검색에도 이 VO 를 쓴다 (categoryCode: 분류 탭, searchKeyword: 질문·답변 검색어).
 */
@Getter
@Setter
public class FaqVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    /** FAQ ID (eGov IdGnr 채번: FAQ_0000000001) */
    private String faqId;

    /** 분류 (공통코드 FAQ_CATEGORY) */
    @NotBlank(message = "분류를 선택하세요.")
    private String categoryCode;
    private String categoryName;   // 조인용

    @NotBlank(message = "질문을 입력하세요.")
    @Size(max = 300, message = "질문은 300자 이내로 입력하세요.")
    private String question;

    @NotBlank(message = "답변을 입력하세요.")
    @Size(max = 2000, message = "답변은 2000자 이내로 입력하세요.")
    private String answer;

    /** 같은 분류 안에서 보여 줄 순서 (작을수록 위) */
    @NotNull(message = "표시 순서를 입력하세요.")
    @Min(value = 1, message = "표시 순서는 1~999 사이로 입력하세요.")
    @Max(value = 999, message = "표시 순서는 1~999 사이로 입력하세요.")
    private Integer sortOrder;

    private String useYn;          // 논리 삭제 플래그 (N 이면 삭제된 글)
    private String writerId;
    private String writerName;     // 조인용
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
