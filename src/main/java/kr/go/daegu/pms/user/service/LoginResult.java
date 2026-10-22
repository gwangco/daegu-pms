package kr.go.daegu.pms.user.service;

import java.util.Arrays;

/** 접속 기록 결과 코드 */
public enum LoginResult {

    SUCCESS("로그인"),
    FAIL_PASSWORD("비밀번호 오류"),
    FAIL_UNKNOWN_ID("없는 아이디"),
    LOCKED("잠긴 계정 시도"),
    DISABLED("중지된 계정 시도"),
    LOGOUT("로그아웃");

    private final String label;

    LoginResult(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        return Arrays.stream(values()).filter(r -> r.name().equals(code))
                .map(LoginResult::getLabel).findFirst().orElse(code);
    }
}
