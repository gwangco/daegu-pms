package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 공통 레이아웃·정적 리소스·CSP·웹 접근성 기본 요소 검증 */
class WebUiIntegrationTest extends IntegrationTestSupport {

    @Autowired ServerProperties serverProperties;

    private static final RequestPostProcessor ANONYMOUS = request -> request;

    /** 역할별로 자주 쓰는 화면 (화면 → 요청 사용자) */
    private static final Map<String, RequestPostProcessor> PAGES = Map.ofEntries(
            Map.entry("/", ANONYMOUS),
            Map.entry("/login", ANONYMOUS),
            Map.entry("/privacy", ANONYMOUS),
            Map.entry("/signup", ANONYMOUS),
            Map.entry("/announcements", ANONYMOUS),
            Map.entry("/announcements/13", ANONYMOUS),
            Map.entry("/notices", ANONYMOUS),
            Map.entry("/notices/1", ANONYMOUS),
            Map.entry("/faqs", ANONYMOUS),
            Map.entry("/faqs?faqClCode=SETTLE", user("manager1").roles("MANAGER")),
            Map.entry("/notices/new", user("manager1").roles("MANAGER")),
            Map.entry("/notices/1/edit", user("manager1").roles("MANAGER")),
            Map.entry("/faqs/new", user("manager1").roles("MANAGER")),
            Map.entry("/applications", user("manager1").roles("MANAGER")),
            Map.entry("/applications/1", user("manager1").roles("MANAGER")),
            Map.entry("/announcements/new", user("manager1").roles("MANAGER")),
            Map.entry("/settlements/application/1", user("company1").roles("COMPANY")),
            Map.entry("/applications/apply?announcementId=15", user("company1").roles("COMPANY")),
            Map.entry("/evaluator/evaluations", user("evaluator1").roles("EVALUATOR")),
            Map.entry("/evaluator/evaluations/3", user("evaluator1").roles("EVALUATOR")),
            Map.entry("/admin/users", user("admin").roles("ADMIN")),
            Map.entry("/mypage", user("company1").roles("COMPANY")),
            Map.entry("/settlements/review", user("manager1").roles("MANAGER")),
            Map.entry("/admin/login-history", user("admin").roles("ADMIN")),
            Map.entry("/admin/privacy-log", user("admin").roles("ADMIN")));

    @Test
    @DisplayName("CSS·JS·글꼴을 외부 CDN 이 아닌 WebJars/정적 경로에서 비로그인으로 받을 수 있다")
    void staticResourcesAreServedLocally() throws Exception {
        for (String path : List.of("/webjars/bootstrap/dist/css/bootstrap.min.css",
                "/webjars/bootstrap/dist/js/bootstrap.bundle.min.js",
                "/webjars/bootstrap-icons/font/bootstrap-icons.min.css",
                "/webjars/bootstrap-icons/font/fonts/bootstrap-icons.woff2",
                "/css/dpms.css", "/js/dpms.js", "/favicon.svg", "/images/logo.svg",
                "/fonts/pretendard-gov/PretendardGOV-Regular.subset.woff2",
                "/fonts/pretendard-gov/PretendardGOV-Medium.subset.woff2",
                "/fonts/pretendard-gov/PretendardGOV-Bold.subset.woff2")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("정적 파일: 화면에는 내용 해시가 붙은 주소로 나가고 1년 캐시, 화면(HTML)은 캐시하지 않는다")
    void staticResourcesAreVersionedAndCached() throws Exception {
        String html = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();
        assertThat(html)
                .containsPattern("/css/dpms-[0-9a-f]{32}\\.css")
                .containsPattern("/js/dpms-[0-9a-f]{32}\\.js")
                .containsPattern("/fonts/pretendard-gov/PretendardGOV-Regular\\.subset-[0-9a-f]{32}\\.woff2");

        // CSS 안의 글꼴 주소도 해시 주소로 바뀌어야 캐시가 맞는다 (CssLinkResourceTransformer)
        String css = mockMvc.perform(get("/css/dpms.css"))
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andReturn().getResponse().getContentAsString();
        assertThat(css).containsPattern("PretendardGOV-Bold\\.subset-[0-9a-f]{32}\\.woff2");

        // 로그인 사용자의 화면이 공용 PC 브라우저에 남지 않도록 HTML 은 계속 no-store
        mockMvc.perform(get("/")).andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("모든 주요 화면: 한국어 lang, 본문 바로가기, CSP 헤더, 인라인 스크립트·이벤트·개발 주석·외부 CDN 없음")
    void layoutAndCspOnEveryPage() throws Exception {
        for (Map.Entry<String, RequestPostProcessor> page : PAGES.entrySet()) {
            MvcResult result = mockMvc.perform(get(page.getKey()).with(page.getValue()))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                    .andReturn();
            String html = result.getResponse().getContentAsString();
            assertThat(html).as(page.getKey())
                    .contains("<html lang=\"ko\"", "href=\"#main\"", "id=\"main\"")
                    .doesNotContain("onclick=", "onsubmit=", "oninput=", "<script>", "style=\"", "cdn.jsdelivr.net", "<!--");
        }
    }

    @Test
    @DisplayName("주 메뉴는 현재 위치를 aria-current 로 알려 준다")
    void currentMenuIsMarked() throws Exception {
        mockMvc.perform(get("/notices"))
                .andExpect(content().string(containsString("aria-current=\"page\">공지사항</a>")));
        mockMvc.perform(get("/announcements/13"))
                .andExpect(content().string(containsString("aria-current=\"page\">사업공고</a>")));
        // 시스템 관리 아래 하위 메뉴도 지금 화면만 표시한다
        mockMvc.perform(get("/admin/login-history").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("aria-current=\"page\">접속 기록</a>")))
                .andExpect(content().string(not(containsString("aria-current=\"page\">사용자 관리</a>"))));
    }

    @Test
    @DisplayName("접수 중 공고는 마감까지 남은 날을 업무 날짜(Clock) 기준 D-n 으로 보여 준다")
    void openAnnouncementShowsDday() throws Exception {
        // 고정 날짜 2026-10-21, 공고 13 마감 2026-11-30 → 40일 전
        mockMvc.perform(get("/announcements/13"))
                .andExpect(content().string(containsString(">D-40</span>")))
                .andExpect(content().string(containsString("마감 40일 전")));
        // 마감된 공고에는 표시하지 않는다
        mockMvc.perform(get("/announcements/1"))
                .andExpect(content().string(not(containsString("class=\"dday"))));
    }

    @Test
    @DisplayName("정산 승인은 되돌릴 수 없으므로 확인 창을 거친다")
    void settlementApprovalAsksConfirmation() throws Exception {
        mockMvc.perform(get("/settlements/application/1").with(user("manager1").roles("MANAGER")))
                .andExpect(content().string(containsString("data-confirm=\"이 집행 건을 승인하시겠습니까?")));
    }

    @Test
    @DisplayName("오류 화면도 공통 스타일을 쓰고 내부 정보 없이 안내 문구만 보여 준다")
    void errorPageUsesSimpleLayout() throws Exception {
        mockMvc.perform(get("/announcements/99999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("페이지를 찾을 수 없습니다")))
                .andExpect(content().string(containsString("존재하지 않는 공고입니다.")))
                .andExpect(content().string(containsString("/css/dpms-")));
    }

    @Test
    @DisplayName("클릭재킹 방지: 어떤 사이트도 화면을 iframe 으로 감쌀 수 없다")
    void framingIsDenied() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }

    @Test
    @DisplayName("세션: URL 에 세션 ID 를 붙이지 않고, 쿠키는 HttpOnly·SameSite=Lax, 30분 만료")
    void sessionCookiePolicy() {
        Session session = serverProperties.getServlet().getSession();
        assertThat(session.getTrackingModes()).containsExactly(Session.SessionTrackingMode.COOKIE);
        assertThat(session.getTimeout()).hasMinutes(30);
        assertThat(session.getCookie().getHttpOnly()).isTrue();
        assertThat(session.getCookie().getSameSite()).isEqualTo(Cookie.SameSite.LAX);
    }
}
