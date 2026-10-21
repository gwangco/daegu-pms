package kr.go.daegu.pms.cmm.exception;

import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.exception.handler.ExceptionHandler;

/**
 * 서비스 계층 예외 후처리기 (eGovFrame ExceptionHandler 구현).
 *
 * ExceptionTransfer 가 ServiceImpl 에서 발생한 예상치 못한 예외를 넘겨주면 호출된다.
 * 실무에서는 이 자리에서 장애 알림(메일/SMS/메신저), 장애 이력 테이블 저장 등을 수행한다.
 * 이 프로젝트에서는 어느 서비스 메서드에서 장애가 났는지 로그로 남긴다.
 *
 * ※ 예외 메시지·스택트레이스는 서버 로그에만 남기고 사용자 화면에는 노출하지 않는다 (오류 메시지 정보노출 방지)
 */
@Slf4j
public class EgovServiceExceptionHandler implements ExceptionHandler {

    @Override
    public void occur(Exception ex, String packageName) {
        log.error("[서비스 장애] 위치={}, 예외={}", packageName, ex.getClass().getName());
    }
}
