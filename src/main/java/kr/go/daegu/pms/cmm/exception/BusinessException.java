package kr.go.daegu.pms.cmm.exception;

import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;

/**
 * 업무 규칙 위반을 나타내는 사용자 정의 예외.
 * 예) 예산 초과 집행, 접수기간 외 신청, 허용되지 않는 상태 변경
 *
 * 전자정부 실행환경의 BaseRuntimeException 을 상속해 eGov 예외 계층에 포함시켰다.
 * (eGov 의 EgovBizException 은 체크 예외라 모든 메서드에 throws 선언이 필요하므로,
 *  최신 eGov 5 샘플처럼 런타임 예외 계열을 사용한다)
 *
 * 시스템 오류(NullPointerException, DB 장애 등)와 구분하여
 * 사용자에게 "무엇이 잘못되었는지" 안내 메시지를 그대로 보여줄 수 있는 예외만 이 타입으로 던진다.
 */
public class BusinessException extends BaseRuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
