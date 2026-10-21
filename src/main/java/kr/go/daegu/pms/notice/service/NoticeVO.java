package kr.go.daegu.pms.notice.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 공지사항 VO.
 *
 * eGovFrame VS Code 확장의 CRUD 코드 생성기(DDL: docs/egov-codegen/notice.ddl.sql)로 만든 NoticeVO 를 바탕으로 수정했다.
 * - 생성기의 NoticeDefaultVO(검색/페이징 필드) 대신 프로젝트 공통 ComDefaultVO 를 상속 (중복 제거)
 * - java.util.Date → java.time.LocalDateTime
 * - ptl-reactive 모듈의 @EgovNullCheck 대신 Bean Validation(@NotBlank 등) 사용
 * - Lombok 으로 getter/setter 대체
 */
@Getter
@Setter
public class NoticeVO extends ComDefaultVO {

    private static final long serialVersionUID = 1L;

    public static final String YES = "Y";
    public static final String NO = "N";

    /** 공지 ID (eGov IdGnr 채번: NTC_0000000001) */
    private String noticeId;

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력하세요.")
    private String title;

    @NotBlank(message = "내용을 입력하세요.")
    @Size(max = 4000, message = "내용은 4000자 이내로 입력하세요.")
    private String content;

    /** 중요 공지 여부 (Y: 목록 상단 고정) */
    @Pattern(regexp = "[YN]", message = "중요 공지 여부 값이 올바르지 않습니다.")
    private String importantYn;

    private int viewCount;
    private String useYn;          // 논리 삭제 플래그 (N 이면 삭제된 글)
    private String writerId;
    private String writerName;     // 조인용
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
