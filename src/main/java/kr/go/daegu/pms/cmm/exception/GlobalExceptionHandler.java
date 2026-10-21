package kr.go.daegu.pms.cmm.exception;

import kr.go.daegu.pms.cmm.message.AppMessages;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 컨트롤러 전역 예외 처리기.
 *
 * - BusinessException  : 400 + 안내 메시지 화면 (error/business.html)
 * - NotFoundException  : 404 화면
 * - 그 외 예외          : Spring Boot 기본 오류 처리(/error)로 넘어가 templates/error/{상태코드}.html 렌더링
 *
 * ※ Exception.class 를 통째로 잡지 않는 이유:
 *    @PreAuthorize 가 던지는 AccessDeniedException 까지 삼켜버리면
 *    "비로그인 → 로그인 페이지 이동" 같은 Spring Security 기본 동작이 깨지기 때문이다.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AppMessages messages;

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException e, Model model, HttpServletResponse response) {
        log.info("[업무 규칙 위반] {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/business";
    }

    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException e, Model model, HttpServletResponse response) {
        log.info("[데이터 없음] {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("errorMessage", e.getMessage());
        return "error/404";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUpload(Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("errorMessage", messages.text("file.size.exceeded"));
        return "error/business";
    }
}
