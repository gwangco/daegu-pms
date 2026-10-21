package kr.go.daegu.pms.common.paging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationInfoTest {

    private PaginationInfo create(int currentPage, int totalCount) {
        PaginationInfo info = new PaginationInfo();
        info.setCurrentPageNo(currentPage);
        info.setRecordCountPerPage(10);
        info.setPageSize(10);
        info.setTotalRecordCount(totalCount);
        return info;
    }

    @Test
    @DisplayName("총 25건, 2페이지 → 3페이지 중 2페이지, OFFSET 10")
    void basicPaging() {
        PaginationInfo info = create(2, 25);

        assertThat(info.getTotalPageCount()).isEqualTo(3);
        assertThat(info.getFirstPageNoOnPageList()).isEqualTo(1);
        assertThat(info.getLastPageNoOnPageList()).isEqualTo(3);
        assertThat(info.getFirstRecordIndex()).isEqualTo(10);
        assertThat(info.isHasPreviousBlock()).isFalse();
        assertThat(info.isHasNextBlock()).isFalse();
    }

    @Test
    @DisplayName("총 250건, 13페이지 → 페이지 블록 11~20, 이전/다음 블록 존재")
    void pageBlock() {
        PaginationInfo info = create(13, 250);

        assertThat(info.getTotalPageCount()).isEqualTo(25);
        assertThat(info.getFirstPageNoOnPageList()).isEqualTo(11);
        assertThat(info.getLastPageNoOnPageList()).isEqualTo(20);
        assertThat(info.isHasPreviousBlock()).isTrue();
        assertThat(info.isHasNextBlock()).isTrue();
    }

    @Test
    @DisplayName("0건이어도 1페이지로 계산되고, 음수 페이지 번호는 1로 보정")
    void edgeCases() {
        PaginationInfo info = create(-5, 0);

        assertThat(info.getCurrentPageNo()).isEqualTo(1);
        assertThat(info.getTotalPageCount()).isEqualTo(1);
        assertThat(info.getFirstRecordIndex()).isZero();
    }
}
