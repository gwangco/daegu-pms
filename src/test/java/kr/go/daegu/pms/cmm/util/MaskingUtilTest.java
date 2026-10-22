package kr.go.daegu.pms.cmm.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingUtilTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "010-1234-5678, 010-****-5678",
            "02-123-4567, 02-***-4567",
            "01012345678, 010****5678",
            "0537401234, 053***1234",
            "12345, *****"
    })
    @DisplayName("전화번호는 가운데 국번을 가린다")
    void phone(String input, String expected) {
        assertThat(MaskingUtil.phone(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "ceo@mobility.example.com, ce*@mobility.example.com",
            "ab@example.com, a*@example.com",
            "a@example.com, a@example.com",
            "manager1@example.com, ma******@example.com",
            "not-an-email, ************"
    })
    @DisplayName("이메일은 아이디 앞 두 글자만 남긴다")
    void email(String input, String expected) {
        assertThat(MaskingUtil.email(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "504-81-98765, 504-81-*****",
            "5048198765, 50481*****"
    })
    @DisplayName("사업자등록번호는 끝 5자리를 가린다")
    void businessNo(String input, String expected) {
        assertThat(MaskingUtil.businessNo(input)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "이대표, 이*표",
            "홍길, 홍*",
            "남궁민수, 남**수",
            "김, *"
    })
    @DisplayName("이름은 첫 글자와 마지막 글자만 남긴다")
    void name(String input, String expected) {
        assertThat(MaskingUtil.name(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("값이 없으면 그대로 돌려준다")
    void blank() {
        assertThat(MaskingUtil.phone(null)).isNull();
        assertThat(MaskingUtil.email("")).isEmpty();
        assertThat(MaskingUtil.businessNo(null)).isNull();
        assertThat(MaskingUtil.name(null)).isNull();
    }
}
