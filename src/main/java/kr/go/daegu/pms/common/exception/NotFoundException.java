package kr.go.daegu.pms.common.exception;

/**
 * 요청한 데이터(공고, 신청서, 파일 등)가 존재하지 않을 때 던지는 예외. 404 화면으로 연결된다.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
