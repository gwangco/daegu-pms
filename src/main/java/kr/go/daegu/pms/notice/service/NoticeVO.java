package kr.go.daegu.pms.notice.service;

import egovframework.com.cop.bbs.service.BoardVO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 공지사항 등록·수정 화면 VO.
 *
 * 전자정부 공통컴포넌트 게시판(cop.bbs)의 게시물 VO(BoardVO)를 그대로 상속하고, 공지 화면에 필요한 입력 검증만 덧붙인다.
 * (공통컴포넌트 원본은 ptl-reactive 모듈의 @EgovNullCheck 를 쓰지만 D-PMS 는 Bean Validation 과 안내 문구를 쓴다)
 *
 * 검증 어노테이션을 getter 에 단 이유: 검사할 필드(nttSj, nttCn)가 부모 클래스(Board)에 private 으로 선언되어 있어서
 * 필드에 직접 달 수 없기 때문이다. getter 에 단 제약도 같은 이름(nttSj)의 입력 오류로 화면에 표시된다.
 */
@SuppressWarnings("serial")
public class NoticeVO extends BoardVO {

    public static final String YES = "Y";
    public static final String NO = "N";

    @Override
    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이내로 입력하세요.")
    public String getNttSj() {
        return super.getNttSj();
    }

    @Override
    @NotBlank(message = "내용을 입력하세요.")
    @Size(max = 4000, message = "내용은 4000자 이내로 입력하세요.")
    public String getNttCn() {
        return super.getNttCn();
    }

    /** 중요 공지 여부 (Y: 목록 상단 고정). 체크하지 않으면 값이 오지 않아 null */
    @Override
    @Pattern(regexp = "[YN]", message = "중요 공지 여부 값이 올바르지 않습니다.")
    public String getNoticeAt() {
        return super.getNoticeAt();
    }
}
