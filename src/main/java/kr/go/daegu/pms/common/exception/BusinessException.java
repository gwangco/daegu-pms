package kr.go.daegu.pms.common.exception;

/**
 * 업무 규칙 위반을 나타내는 사용자 정의 예외.
 * 예) 예산 초과 집행, 접수기간 외 신청, 허용되지 않는 상태 변경
 *
 * 시스템 오류(NullPointerException, DB 장애 등)와 구분하여
 * 사용자에게 "무엇이 잘못되었는지" 안내 메시지를 그대로 보여줄 수 있는 예외만 이 타입으로 던진다.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
