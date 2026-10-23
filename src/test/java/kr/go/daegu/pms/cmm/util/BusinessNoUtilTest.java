package kr.go.daegu.pms.cmm.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessNoUtilTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "1234567891, 123-45-67891",
            "123-45-67891, 123-45-67891",
            "'123 45 67891', 123-45-67891",
            "' 123-45-67891 ', 123-45-67891"
    })
    @DisplayName("하이픈·공백을 빼고 숫자 10자리면 000-00-00000 형식으로 맞춘다")
    void normalize(String input, String expected) {
        assertThat(BusinessNoUtil.normalize(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"", "123456789", "12345678901", "123-45-6789O", "１２３-45-67891"})
    @DisplayName("10자리 숫자가 아니면(전각 숫자·영문 O 포함) null")
    void normalizeRejects(String input) {
        assertThat(BusinessNoUtil.normalize(input)).isNull();
        assertThat(BusinessNoUtil.normalize(null)).isNull();
    }

    @Test
    @DisplayName("검증번호 계산: 123-45-6789? 의 마지막 자리는 1")
    void checkDigitByHand() {
        // 1×1 + 2×3 + 3×7 + 4×1 + 5×3 + 6×7 + 7×1 + 8×3 + 9×5 = 165
        // 9번째 자리(9) × 5 ÷ 10 의 몫 = 4 → 합 169 → 10 - 9 = 1
        assertThat(BusinessNoUtil.isValid("123-45-67891")).isTrue();
        assertThat(BusinessNoUtil.isValid("123-45-67890")).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"504-81-98761", "504-86-54322"})
    @DisplayName("데모 기업회원 번호(V4_1)는 검증번호가 맞다")
    void demoNumbersAreValid(String businessNo) {
        assertThat(BusinessNoUtil.isValid(businessNo)).isTrue();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"504-81-98765", "504-86-54321", "000-00-00000", "123-00-67891", "abc"})
    @DisplayName("검증번호가 틀리거나, 쓰지 않는 코드(세무서 000·구분 00)거나, 형식이 아니면 거부")
    void invalid(String businessNo) {
        assertThat(BusinessNoUtil.isValid(businessNo)).isFalse();
    }
}
