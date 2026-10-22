package kr.go.daegu.pms.cmm.pagination;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.AbstractPaginationRenderer;
import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Bootstrap 5 모양의 페이지 번호 바를 그리는 전자정부 PaginationRenderer.
 *
 * eGov 의 AbstractPaginationRenderer 는 "처음/이전/현재/다른/다음/마지막" 6가지 라벨(HTML 조각)만 정하면
 * PaginationInfo 계산 결과에 맞춰 페이지 번호 바를 조립해 준다. 이 클래스는 라벨을 Bootstrap 마크업으로 정의한 것이다.
 * (JSP 의 &lt;ui:pagination&gt; 태그, eGov Thymeleaf 샘플의 EgovKrdsPaginationRenderer 와 같은 확장 지점)
 *
 * 라벨의 {0} = 페이지 이동 JS 함수명, {1} = 이동할 페이지 번호, {2} = 화면에 표시할 번호 (MessageFormat 형식)
 * eGov 기본 샘플은 onclick="{0}({1})" 로 함수를 부르지만, 이 프로젝트는 CSP(script-src 'self') 때문에 인라인 이벤트를 쓰지 않는다.
 * 대신 data-page="{1}" 만 출력하고 /js/dpms.js 가 클릭을 받아 검색 폼의 pageIndex 를 바꾼다.
 *
 * [스레드 안전] 이 객체는 싱글톤 Bean 이므로 요청마다 부모의 라벨 필드를 바꾸지 않고,
 * 라벨을 지역 변수로 만들어 doRender(...) 에 넘긴다. (공유 필드를 바꾸면 동시 요청끼리 라벨이 섞일 수 있다)
 *
 * [보안] 결과 HTML 은 th:utext 로 출력되므로 라벨에는 사용자 입력이 들어가면 안 된다.
 * 들어가는 값은 정수(페이지 번호)와 템플릿에 고정된 JS 함수명, 메시지 번들의 라벨뿐이다.
 * MessageFormat 에서 작은따옴표(')는 이스케이프 문자이므로 라벨에 쓰지 않는다.
 */
public class EgovBootstrapPaginationRenderer extends AbstractPaginationRenderer {

    private static final String CURRENT_PAGE_LABEL =
            "<li class=\"page-item active\"><span class=\"page-link\" aria-current=\"page\">{0}</span></li>";
    private static final String OTHER_PAGE_LABEL =
            "<li class=\"page-item\"><a class=\"page-link\" href=\"#\" data-page=\"{1}\" aria-label=\"{2}쪽\">{2}</a></li>";

    private final MessageSource messageSource;

    public EgovBootstrapPaginationRenderer(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String renderPagination(PaginationInfo paginationInfo, String jsFunction) {
        if (paginationInfo == null || paginationInfo.getTotalRecordCount() <= 0) {
            return "";   // eGov PaginationInfo 는 0건이면 전체 페이지 수를 0 으로 계산하므로 번호 바를 그리지 않는다
        }
        return doRender(paginationInfo, jsFunction,
                moveLink("&laquo;", label("page.first", "처음")),
                moveLink("&lsaquo;", label("page.prev", "이전")),
                CURRENT_PAGE_LABEL,
                OTHER_PAGE_LABEL,
                moveLink("&rsaquo;", label("page.next", "다음")),
                moveLink("&raquo;", label("page.last", "마지막")));
    }

    /** 처음/이전/다음/마지막 버튼. 기호는 화면낭독기가 읽지 않도록 숨기고 라벨을 aria-label 로 준다 */
    private static String moveLink(String symbol, String label) {
        return "<li class=\"page-item\"><a class=\"page-link\" href=\"#\" data-page=\"{1}\" title=\"" + label
                + "\" aria-label=\"" + label + "\"><span aria-hidden=\"true\">" + symbol + "</span></a></li>";
    }

    private String label(String code, String defaultText) {
        return messageSource.getMessage(code, null, defaultText, LocaleContextHolder.getLocale());
    }
}
