package kr.go.daegu.pms.cmm.pagination;

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
 */
@Component
@RequiredArgsConstructor
public class PagingHelper {

    private final EgovPropertyService propertiesService;

    /** 검색 VO 에 OFFSET/LIMIT 을 세팅하고 PaginationInfo 를 반환. 총 건수는 조회 후 setTotalRecordCount 로 채운다 */
    public PaginationInfo prepare(ComDefaultVO searchVO) {
        searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
        searchVO.setPageSize(propertiesService.getInt("pageSize"));

        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(searchVO.getPageUnit());
        paginationInfo.setPageSize(searchVO.getPageSize());

        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setLastIndex(paginationInfo.getLastRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
        return paginationInfo;
    }
}
