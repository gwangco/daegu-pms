package kr.go.daegu.pms.cmm.service;

import lombok.Getter;
import lombok.Setter;

/**
 * 목록 검색 + 페이징 공통 VO (전자정부 표준프레임워크 샘플의 DefaultVO 와 같은 역할).
 *
 * - pageIndex          : 화면에서 넘어오는 현재 페이지 번호
 * - firstIndex         : SQL OFFSET (서버에서 PaginationInfo 로 계산)
 * - recordCountPerPage : SQL LIMIT  (서버에서 설정. 0 이면 페이징 없이 전체 조회 → 엑셀 다운로드용)
 */
@Getter
@Setter
public class ComDefaultVO {

    private int pageIndex = 1;
    private int firstIndex = 0;
    private int recordCountPerPage = 0;

    /** 검색어 */
    private String keyword;

    public void setPageIndex(int pageIndex) {
        this.pageIndex = Math.max(1, pageIndex);
    }
}
