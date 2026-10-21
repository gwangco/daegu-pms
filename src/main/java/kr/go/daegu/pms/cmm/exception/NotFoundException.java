package kr.go.daegu.pms.cmm.exception;

import org.egovframe.rte.fdl.cmmn.exception.BaseRuntimeException;

/**
 * 요청한 데이터(공고, 신청서, 파일 등)가 존재하지 않을 때 던지는 예외. 404 화면으로 연결된다.
 * (eGov 메시지 키 info.nodata.msg 에 해당하는 상황)
 */
public class NotFoundException extends BaseRuntimeException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }
}
