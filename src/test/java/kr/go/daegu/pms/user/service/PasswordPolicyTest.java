package kr.go.daegu.pms.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private static final String USER_ID = "newcorp1";

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Dpms!2026",          // 대문자·소문자·특수문자·숫자 4종류, 9자
            "dpms!2580",          // 소문자·특수문자·숫자 3종류, 9자
            "daegu#159",          // 3종류, 9자 (1·5·9 는 연속이 아님)
            "Daegumobility",      // 대문자·소문자 2종류, 13자
            "daegu15937"          // 소문자·숫자 2종류, 10자
    })
    @DisplayName("3종류 8자 이상 또는 2종류 10자 이상이면 통과")
    void accepts(String password) {
        assertThat(PasswordPolicy.check(password, USER_ID)).isEmpty();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "'Dpms!26', LENGTH",               // 4종류지만 7자
            "'daegu1593', LENGTH",             // 2종류 9자 (10자 필요)
            "'daegumobility', LENGTH",         // 1종류
            "'dpms 2580!', CHARACTERS",        // 공백
            "'대구dpms!2580', CHARACTERS",      // 한글 (BCrypt 72바이트 제한과 1자=1바이트 가정이 깨짐)
            "'Newcorp1!x', CONTAINS_USER_ID",  // 대소문자를 바꿔도 아이디 포함
            "'dpms!2555x', REPEATED",          // 555
            "'dpms!1234x', SEQUENTIAL",        // 1234
            "'xdcba!2580', SEQUENTIAL",        // dcba (내림차순)
            "'zABCD!258', SEQUENTIAL"          // ABCD (대소문자 무시)
    })
    @DisplayName("규칙을 어기면 처음 어긴 규칙을 알려 준다")
    void rejects(String password, PasswordPolicy.Violation expected) {
        assertThat(PasswordPolicy.check(password, USER_ID)).contains(expected);
    }

    @Test
    @DisplayName("최대 64자: BCrypt 가 앞 72바이트만 쓰므로 그보다 긴 비밀번호는 받지 않는다")
    void maxLength() {
        String sixtyFour = "Dp!7" + "x9".repeat(30);
        assertThat(sixtyFour).hasSize(64);
        assertThat(PasswordPolicy.check(sixtyFour, USER_ID)).isEmpty();
        assertThat(PasswordPolicy.check(sixtyFour + "y", USER_ID)).contains(PasswordPolicy.Violation.LENGTH);
    }

    @Test
    @DisplayName("빈 값·null 은 거부, 아이디가 없으면(아이디 칸 오류) 아이디 포함 검사는 건너뛴다")
    void emptyValues() {
        assertThat(PasswordPolicy.check(null, USER_ID)).contains(PasswordPolicy.Violation.CHARACTERS);
        assertThat(PasswordPolicy.check("", USER_ID)).contains(PasswordPolicy.Violation.CHARACTERS);
        assertThat(PasswordPolicy.check("dpms!2580", null)).isEmpty();
    }
}
