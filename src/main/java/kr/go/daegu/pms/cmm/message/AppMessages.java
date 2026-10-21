package kr.go.daegu.pms.cmm.message;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * eGov 공통 messageSource 로 사용자 안내를 해석한다.
 *
 * 한국어 서비스의 금액·숫자 표기는 서버/브라우저 언어와 무관하게 한국 로케일을 사용한다.
 * 없는 키에는 기본 문자열을 지정하지 않아 설정 누락을 조기에 발견한다.
 * EgovAbstractServiceImpl.processException 은 체크 예외(EgovBizException)를 반환하므로,
 * 기존 HTTP 400/404/403 및 AOP 예외 분류를 유지하도록 런타임 예외를 만든다.
 */
@Component
@RequiredArgsConstructor
public class AppMessages {

    private final MessageSource messageSource;

    public String text(String code, Object... args) {
        return messageSource.getMessage(code, args, Locale.KOREA);
    }

    public BusinessException business(String code, Object... args) {
        return new BusinessException(text(code, args));
    }

    public NotFoundException notFound(String code, Object... args) {
        return new NotFoundException(text(code, args));
    }

    public AccessDeniedException denied(String code, Object... args) {
        return new AccessDeniedException(text(code, args));
    }
}
