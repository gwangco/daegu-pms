package kr.go.daegu.pms.common.paging;

import kr.go.daegu.pms.common.vo.SearchVO;
import kr.go.daegu.pms.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 전자정부 표준 컨트롤러의 페이징 준비 코드를 한 곳에 모은 도우미.
 *
 * eGov 샘플 컨트롤러는 목록 화면마다 아래 코드를 반복한다:
 *   searchVO.setPageUnit(propertiesService.getInt("pageUnit"));
 *   PaginationInfo paginationInfo = new PaginationInfo();
 *   paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
 *   ...
 *   searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
 *   searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
 */
@Component
@RequiredArgsConstructor
public class PagingHelper {

    private final AppProperties appProperties;

    /** 검색 VO 에 OFFSET/LIMIT 을 세팅하고 PaginationInfo 를 반환. 총 건수는 조회 후 setTotalRecordCount 로 채운다 */
    public PaginationInfo prepare(SearchVO searchVO) {
        PaginationInfo paginationInfo = new PaginationInfo();
        paginationInfo.setCurrentPageNo(searchVO.getPageIndex());
        paginationInfo.setRecordCountPerPage(appProperties.paging().pageUnit());
        paginationInfo.setPageSize(appProperties.paging().pageSize());

        searchVO.setFirstIndex(paginationInfo.getFirstRecordIndex());
        searchVO.setRecordCountPerPage(paginationInfo.getRecordCountPerPage());
        return paginationInfo;
    }
}
