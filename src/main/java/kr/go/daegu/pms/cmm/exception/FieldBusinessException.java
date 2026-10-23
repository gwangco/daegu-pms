package kr.go.daegu.pms.cmm.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 입력 칸별 업무 규칙 위반 (칸 이름 → 안내 문구).
 *
 * 회원가입처럼 한 번에 여러 칸을 확인하는 화면에서, 서비스가 모든 위반을 모아 한 번에 돌려줄 때 쓴다.
 * 컨트롤러는 칸마다 BindingResult.rejectValue 로 옮겨 각 입력칸 아래에 보여 준다.
 * BusinessException 을 상속하므로 eGov 예외 후처리에서 장애 로그로 남지 않는다.
 */
public class FieldBusinessException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> fieldErrors;

    public FieldBusinessException(Map<String, String> fieldErrors) {
        super(fieldErrors.values().stream().findFirst().orElse(""));
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
