package kr.go.daegu.pms.cmm.pagination;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationRenderer;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Set;

/**
 * Thymeleaf 에서 eGov 페이지 렌더러를 #egovPaginationRenderer 로 쓸 수 있게 해 주는 Dialect.
 * (eGovFrame Boot Thymeleaf 샘플의 EgovPaginationDialect 와 같은 방식)
 *
 * 사용 예:
 *   &lt;ul class="pagination" th:utext="${#egovPaginationRenderer.renderPagination(paginationInfo, 'fnLinkPage')}"&gt;&lt;/ul&gt;
 *
 * Spring Boot 는 IDialect 타입 Bean 을 자동으로 Thymeleaf 템플릿 엔진에 등록한다.
 */
public class EgovPaginationDialect extends AbstractDialect implements IExpressionObjectDialect {

    public static final String EXPRESSION_OBJECT_NAME = "egovPaginationRenderer";

    private final PaginationRenderer paginationRenderer;

    public EgovPaginationDialect(PaginationRenderer paginationRenderer) {
        super("EgovPaginationDialect");
        this.paginationRenderer = paginationRenderer;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return Set.of(EXPRESSION_OBJECT_NAME);
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return paginationRenderer;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                return true;
            }
        };
    }
}
