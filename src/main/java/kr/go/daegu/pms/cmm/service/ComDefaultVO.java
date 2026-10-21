package kr.go.daegu.pms.cmm.service;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 목록 검색 + 페이징 공통 VO.
 * 전자정부 공통컴포넌트의 ComDefaultVO / 코드생성기의 DefaultVO 와 같은 필드 구성을 따른다.
 *
 * - pageIndex          : 화면에서 넘어오는 현재 페이지 번호
 * - pageUnit           : 한 페이지에 보여줄 게시물 수 (서버가 EgovPropertyService 값으로 설정)
 * - pageSize           : 페이지 번호 블록 크기     (서버가 EgovPropertyService 값으로 설정)
 * - firstIndex         : SQL OFFSET (PaginationInfo 로 계산)
 * - lastIndex          : Oracle ROWNUM 방식 페이징에서 쓰는 끝 번호 (PaginationInfo 로 계산)
 * - recordCountPerPage : SQL LIMIT. 0 이면 페이징 없이 전체 조회 (엑셀 다운로드용)
 *
 * ※ pageUnit/pageSize/firstIndex/lastIndex/recordCountPerPage 는 GlobalBindingAdvice 에서
 *   요청 파라미터 바인딩을 차단한다 (?recordCountPerPage=0 으로 전체 데이터를 빼가는 시도 방지)
 */
@Getter
@Setter
public class ComDefaultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 검색 조건 (검색 대상 컬럼 선택 등에 사용) */
    private String searchCondition;

    /** 검색어 */
    private String searchKeyword;

    private int pageIndex = 1;
    private int pageUnit = 10;
    private int pageSize = 10;
    private int firstIndex = 0;
    private int lastIndex = 0;
    private int recordCountPerPage = 0;

    public void setPageIndex(int pageIndex) {
        // eGov PaginationInfo 는 0 이하 페이지를 보정하지 않으므로 여기서 1 이상으로 보정 (음수 OFFSET 방지)
        this.pageIndex = Math.max(1, pageIndex);
    }
}
