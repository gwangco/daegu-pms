package kr.go.daegu.pms.cmm.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNoUtilTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "01012345678, 010-1234-5678",
            "010-1234-5678, 010-1234-5678",
            "'010 1234 5678', 010-1234-5678",
            "0537411234, 053-741-1234",
            "021234567, 02-123-4567",
            "0212345678, 02-1234-5678",
            "07012345678, 070-1234-5678"
    })
    @DisplayName("숫자만 남겨 국번 규칙대로 하이픈을 넣는다")
    void normalize(String input, String expected) {
        assertThat(PhoneNoUtil.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"", "1012345678", "0101234567890", "0212345", "010-1234-567a", "+82-10-1234-5678"})
    @DisplayName("국내 번호 형식이 아니면 null (0 으로 시작하지 않음, 자릿수, 문자)")
    void rejects(String input) {
        assertThat(PhoneNoUtil.normalize(input)).isNull();
    }
}
