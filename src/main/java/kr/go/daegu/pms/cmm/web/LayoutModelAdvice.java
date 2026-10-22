package kr.go.daegu.pms.cmm.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공통 레이아웃(layout/default.html)이 쓰는 값.
 * 주 메뉴에서 지금 보고 있는 메뉴를 강조하고 aria-current="page" 를 붙이는 데 사용한다.
 */
@ControllerAdvice
public class LayoutModelAdvice {

    /** URL 접두어 → 메뉴 이름 (위에서부터 먼저 맞는 것을 쓴다) */
    private static final Map<String, String> MENUS = new LinkedHashMap<>();

    static {
        MENUS.put("/notices", "notice");
        MENUS.put("/announcements", "announcement");
        MENUS.put("/applications", "application");
        MENUS.put("/evaluator", "evaluation");
        MENUS.put("/settlements/application", "application");   // 과제별 정산 화면은 신청 현황에서 들어간다
        MENUS.put("/settlements", "settlement");
        MENUS.put("/admin", "admin");
        MENUS.put("/mypage", "mypage");
    }

    @ModelAttribute("currentMenu")
    public String currentMenu(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return MENUS.entrySet().stream()
                .filter(menu -> path.equals(menu.getKey()) || path.startsWith(menu.getKey() + "/"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");
    }
}
