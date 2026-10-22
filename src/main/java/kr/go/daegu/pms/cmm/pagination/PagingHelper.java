package kr.go.daegu.pms.cmm.pagination;

import egovframework.com.cop.bbs.service.BoardVO;
import egovframework.com.uss.olh.faq.service.FaqDefaultVO;
import kr.go.daegu.pms.cmm.service.ComDefaultVO;
import lombok.RequiredArgsConstructor;
import org.egovframe.rte.fdl.property.EgovPropertyService;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Component;

/**
 * 전자정부 표준 컨트롤러의 페이징 준비 코드를 한 곳에 모은 도우미.
 *
 * eGov 코드생성기가 만드는 컨트롤러는 목록 화면마다 아래 코드를 반복한다. 그 순서를 그대로 따른다:
 * <pre>
 *   searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
 *   searchVO.setPageSize(propertiesService.getInt("pageSize"));
 *
 *   PaginationInfo paginationInfo = new PaginationInfo();
 *   paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
 *   paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
 *   paginationInfo.setPageSize(searchVO.getPageSize());
 *
 *   searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
 *   searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
 *   searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
 *   ... 목록 조회 → 총 건수 조회 → paginationInfo.setTotalRecordCount(totCnt)
 * </pre>
 *
 * 공통컴포넌트 검색 VO(게시판 BoardVO, FAQ FaqDefaultVO)는 업무 ComDefaultVO 와 필드 이름이 같지만 부모 클래스가 달라
 * 같은 순서의 메서드를 따로 둔다.
 */
@Component
@RequiredArgsConstructor
public class PagingHelper {

    private final EgovPropertyService propertiesService;

    /** 검색 VO 에 OFFSET/LIMIT 을 세팅하고 PaginationInfo 를 반환. 총 건수는 조회 후 setTotalRecordCount 로 채운다 */
    public PaginationInfo prepare(ComDefaultVO searchVO) {
        PaginationInfo paginationInfo = newPaginationInfo(searchVO.getPageIndex());
        searchVO.setPageUnit(paginationInfo.getRecordCountPerPage());
        searchVO.setPageSize(paginationInfo.getPageSize());
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
        return paginationInfo;
    }

    /** 공통컴포넌트 게시판 검색 VO */
    public PaginationInfo prepare(BoardVO searchVO) {
        PaginationInfo paginationInfo = newPaginationInfo(searchVO.getPageIndex() == null ? 1 : searchVO.getPageIndex());
        searchVO.setPageIndex(paginationInfo.getCurrentPageNo());
        searchVO.setPageUnit(paginationInfo.getRecordCountPerPage());
        searchVO.setPageSize(paginationInfo.getPageSize());
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
        return paginationInfo;
    }

    /** 공통컴포넌트 FAQ 검색 VO */
    public PaginationInfo prepare(FaqDefaultVO searchVO) {
        PaginationInfo paginationInfo = newPaginationInfo(searchVO.getPageIndex());
        searchVO.setPageIndex(paginationInfo.getCurrentPageNo());
        searchVO.setPageUnit(paginationInfo.getRecordCountPerPage());
        searchVO.setPageSize(paginationInfo.getPageSize());
        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
        return paginationInfo;
    }

    /** 한 페이지 게시물 수·페이지 블록 크기는 화면 값이 아니라 EgovPropertyService 값. 0 이하 페이지는 1로 보정 (음수 OFFSET 방지) */
    private PaginationInfo newPaginationInfo(int pageIndex) {
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(Math.max(1, pageIndex));
        paginationInfo.setRecordCountPerPage(propertiesService.getInt("pageUnit"));
        paginationInfo.setPageSize(propertiesService.getInt("pageSize"));
        return paginationInfo;
    }
}
