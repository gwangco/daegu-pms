package kr.go.daegu.pms.cmm.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공통 레이아웃(layout/default.html)과 공통 조각(fragments/common.html)이 쓰는 값.
 * - currentMenu / currentPath: 주 메뉴에서 지금 보고 있는 메뉴를 강조하고 aria-current="page" 를 붙인다
 * - today: 공고 마감 D-day 계산. 서버 기본 시간대가 아니라 업무 Clock(한국 시간)을 쓴다
 */
@ControllerAdvice
@RequiredArgsConstructor
public class LayoutModelAdvice {

    /** URL 접두어 → 메뉴 이름 (위에서부터 먼저 맞는 것을 쓴다) */
    private static final Map<String, String> MENUS = new LinkedHashMap<>();

    static {
        MENUS.put("/notices", "notice");
        MENUS.put("/faqs", "faq");
        MENUS.put("/announcements", "announcement");
        MENUS.put("/applications", "application");
        MENUS.put("/evaluator", "evaluation");
        MENUS.put("/settlements/application", "application");   // 과제별 정산 화면은 신청 현황에서 들어간다
        MENUS.put("/settlements", "settlement");
        MENUS.put("/admin", "admin");
        MENUS.put("/mypage", "mypage");
    }

    private final Clock clock;

    @ModelAttribute("currentMenu")
    public String currentMenu(HttpServletRequest request) {
        String path = currentPath(request);
        return MENUS.entrySet().stream()
                .filter(menu -> path.equals(menu.getKey()) || path.startsWith(menu.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }

    /** 하위 메뉴(시스템 관리 → 사용자 관리·접속 기록) 중 지금 화면을 표시할 때 쓴다 */
    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
