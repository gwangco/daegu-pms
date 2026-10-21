package kr.go.daegu.pms.common.paging;

/**
 * 전자정부 표준프레임워크의 PaginationInfo
 * (egovframework.rte.ptl.mvc.tags.ui.pagination.PaginationInfo) 와 동일한 규칙의 페이징 계산 클래스.
 *
 * 예) 총 25건, recordCountPerPage=10, pageSize=10, currentPageNo=2 일 때
 *   totalPageCount = 3, firstPageNoOnPageList = 1, lastPageNoOnPageList = 3,
 *   firstRecordIndex = 10  → SQL: LIMIT 10 OFFSET 10
 *
 * 사용 순서 (eGov 표준 컨트롤러 패턴):
 *   1) setCurrentPageNo / setRecordCountPerPage / setPageSize
 *   2) getFirstRecordIndex() 로 SQL OFFSET 계산 → 목록 조회
 *   3) 총 건수 조회 후 setTotalRecordCount
 *   4) Model 에 담아 화면의 페이지 번호 바 렌더링
 */
public class PaginationInfo {

    private int currentPageNo = 1;      // 현재 페이지 번호
    private int recordCountPerPage = 10; // 한 페이지당 게시물 수
    private int pageSize = 10;          // 페이지 리스트에 표시할 페이지 번호 개수
    private int totalRecordCount;       // 전체 게시물 수

    public int getCurrentPageNo() {
        return currentPageNo;
    }

    public void setCurrentPageNo(int currentPageNo) {
        this.currentPageNo = Math.max(1, currentPageNo);
    }

    public int getRecordCountPerPage() {
        return recordCountPerPage;
    }

    public void setRecordCountPerPage(int recordCountPerPage) {
        this.recordCountPerPage = Math.max(1, recordCountPerPage);
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, pageSize);
    }

    public int getTotalRecordCount() {
        return totalRecordCount;
    }

    public void setTotalRecordCount(int totalRecordCount) {
        this.totalRecordCount = Math.max(0, totalRecordCount);
    }

    /** 전체 페이지 수 (게시물이 0건이어도 1페이지로 취급) */
    public int getTotalPageCount() {
        return Math.max(1, (totalRecordCount - 1) / recordCountPerPage + 1);
    }

    /** 현재 페이지 블록의 첫 페이지 번호. 예) pageSize=10, 현재 13페이지 → 11 */
    public int getFirstPageNoOnPageList() {
        return ((currentPageNo - 1) / pageSize) * pageSize + 1;
    }

    /** 현재 페이지 블록의 마지막 페이지 번호 (전체 페이지 수를 넘지 않음) */
    public int getLastPageNoOnPageList() {
        return Math.min(getFirstPageNoOnPageList() + pageSize - 1, getTotalPageCount());
    }

    /** SQL OFFSET 값 (0부터 시작) */
    public int getFirstRecordIndex() {
        return (currentPageNo - 1) * recordCountPerPage;
    }

    public boolean isHasPreviousBlock() {
        return getFirstPageNoOnPageList() > 1;
    }

    public boolean isHasNextBlock() {
        return getLastPageNoOnPageList() < getTotalPageCount();
    }
}
