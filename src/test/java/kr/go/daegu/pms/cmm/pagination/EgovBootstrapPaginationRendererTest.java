package kr.go.daegu.pms.cmm.pagination;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class EgovBootstrapPaginationRendererTest {

    private final EgovBootstrapPaginationRenderer renderer = new EgovBootstrapPaginationRenderer(new StaticMessageSource());

    private PaginationInfo info(int currentPage, int totalCount) {
        PaginationInfo info = new PaginationInfo();
        info.setCurrentPageNo(currentPage);
        info.setRecordCountPerPage(10);
        info.setPageSize(10);
        info.setTotalRecordCount(totalCount);
        return info;
    }

    @Test
    @DisplayName("총 25건 2페이지: 1~3 페이지 번호, 2페이지 활성, 페이지 블록이 1개라 처음/이전 버튼 없음")
    void smallList() {
        String html = renderer.renderPagination(info(2, 25), "fnLinkPage");

        assertThat(html).contains("onclick=\"fnLinkPage(1); return false;\">1</a>")
                .contains("<li class=\"page-item active\" aria-current=\"page\"><span class=\"page-link\">2</span></li>")
                .contains("fnLinkPage(3)")
                .doesNotContain("fnLinkPage(4)")
                .doesNotContain("&laquo;");
    }

    @Test
    @DisplayName("총 250건 13페이지: 11~20 블록 + 처음/이전(10)/다음(21)/마지막(25)")
    void pageBlock() {
        String html = renderer.renderPagination(info(13, 250), "fnLinkPage");

        assertThat(html).contains("fnLinkPage(1); return false;\">&laquo;")
                .contains("fnLinkPage(10); return false;\">&lsaquo;")
                .contains("fnLinkPage(21); return false;\">&rsaquo;")
                .contains("fnLinkPage(25); return false;\">&raquo;")
                .contains(">11</a>").contains(">20</a>")
                .doesNotContain(">21</a>");
    }

    @Test
    @DisplayName("0건이면 아무것도 그리지 않는다 (eGov PaginationInfo 는 0건일 때 전체 페이지 수가 0)")
    void empty() {
        assertThat(info(1, 0).getTotalPageCount()).isZero();
        assertThat(renderer.renderPagination(info(1, 0), "fnLinkPage")).isEmpty();
    }

    @Test
    @DisplayName("메시지 번들의 라벨을 버튼 title 로 사용한다")
    void labelsFromMessageSource() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("page.first", java.util.Locale.getDefault(), "First");
        String html = new EgovBootstrapPaginationRenderer(messages).renderPagination(info(13, 250), "fnLinkPage");

        assertThat(html).contains("title=\"First\"");
    }
}
