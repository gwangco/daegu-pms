package kr.go.daegu.pms;

import kr.go.daegu.pms.cmm.crypto.PersonalInfoCipher;
import org.apache.logging.log4j.LogManager;
import org.egovframe.boot.crypto.EgovCryptoProperties;
import org.egovframe.boot.crypto.EgovPasswordEncoder;
import org.egovframe.boot.crypto.service.EgovARIACryptoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** eGov ARIA 암호화(연락처·이메일), 평문 전환, 화면 마스킹 검증 */
class PersonalInfoCryptoIntegrationTest extends IntegrationTestSupport {

    @Autowired PersonalInfoCipher cipher;
    @Autowired EgovARIACryptoService ariaCryptoService;
    @Autowired EgovPasswordEncoder egovPasswordEncoder;
    @Autowired EgovCryptoProperties cryptoProperties;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("기동 시 seed 의 평문 연락처·이메일이 {ARIA} 암호문으로 전환되어 DB 에 원문이 없다")
    void storedAsCipherText() {
        Map<String, Object> row = jdbc.queryForMap("SELECT email, phone FROM users WHERE user_id = 'company1'");
        assertThat((String) row.get("phone")).startsWith("{ARIA}").doesNotContain("4001");
        assertThat((String) row.get("email")).startsWith("{ARIA}").doesNotContain("mobility");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email NOT LIKE '{ARIA}%'", Integer.class)).isZero();

        assertThat(cipher.decrypt((String) row.get("phone"))).isEqualTo("010-0000-4001");
        assertThat(cipher.decrypt((String) row.get("email"))).isEqualTo("ceo@mobility.example.com");
    }

    @Test
    @DisplayName("이미 암호화된 값은 다시 암호화하지 않고, 표식이 없는 값(전환 전 평문)은 그대로 읽는다")
    void idempotentAndMigrationFriendly() {
        String once = cipher.encrypt("010-1234-5678");
        assertThat(cipher.encrypt(once)).isEqualTo(once);
        assertThat(cipher.decrypt("010-1234-5678")).isEqualTo("010-1234-5678");
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isEmpty();
    }

    @Test
    @DisplayName("[eGov ARIA 특성] 같은 평문은 같은 암호문이 된다 (IV 없는 블록 처리) - 값 비교는 되지만 빈도가 드러날 수 있음")
    void deterministic() {
        assertThat(cipher.encrypt("한글 이름 테스트@example.com")).isEqualTo(cipher.encrypt("한글 이름 테스트@example.com"));
        assertThat(cipher.decrypt(cipher.encrypt("한글 이름 테스트@example.com"))).isEqualTo("한글 이름 테스트@example.com");
    }

    @Test
    @DisplayName("[eGov ARIA 특성] 16바이트 배수 평문의 끝이 패딩처럼 보이면 복호화가 잘린다 → 제어문자 값은 암호화를 거부")
    void paddingLikeTailIsRejected() {
        String key = cryptoProperties.getAlgorithmKey();
        byte[] paddingLike = "AAAAAAAAAAAAAAA\u0001".getBytes(StandardCharsets.UTF_8);
        byte[] restored = ariaCryptoService.decrypt(ariaCryptoService.encrypt(paddingLike, key), key);
        assertThat(restored).as("eGov 5.1.0 ARIA 구현이 이 동작을 고치면 이 테스트로 알 수 있다").hasSize(15);

        assertThatThrownBy(() -> cipher.encrypt("AAAAAAAAAAAAAAA\u0001")).isInstanceOf(IllegalArgumentException.class);
        // 일반 텍스트는 16바이트 배수여도 정확히 복원된다
        assertThat(cipher.decrypt(cipher.encrypt("0123456789abcdef"))).isEqualTo("0123456789abcdef");
    }

    @Test
    @DisplayName("키 해시가 맞지 않거나 키 길이가 32자가 아니면 시작할 때 실패한다")
    void rejectsWrongKeyConfiguration() {
        EgovCryptoProperties wrongLength = new EgovCryptoProperties();
        wrongLength.setAlgorithmKey("short-key");
        wrongLength.setAlgorithmKeyHash(cryptoProperties.getAlgorithmKeyHash());
        assertThatThrownBy(() -> new PersonalInfoCipher(ariaCryptoService, egovPasswordEncoder, wrongLength))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32자");

        EgovCryptoProperties wrongKey = new EgovCryptoProperties();
        wrongKey.setAlgorithmKey("A".repeat(32));
        wrongKey.setAlgorithmKeyHash(cryptoProperties.getAlgorithmKeyHash());
        assertThatThrownBy(() -> new PersonalInfoCipher(ariaCryptoService, egovPasswordEncoder, wrongKey))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("일치하지 않습니다");
    }

    @Test
    @DisplayName("eGov 암호화 서비스의 DEBUG 로그는 평문과 키를 출력하므로 기본 설정에서 꺼져 있다")
    void cryptoDebugLogIsOff() {
        assertThat(LogManager.getLogger("org.egovframe.boot.crypto.service.impl.EgovARIACryptoServiceImpl").isDebugEnabled()).isFalse();
    }

    @Test
    @DisplayName("내 정보 화면은 본인에게만 복호화한 전체 연락처·이메일을 보여 준다")
    void myPageShowsOwnContacts() throws Exception {
        mockMvc.perform(get("/mypage").with(user("company1").roles("COMPANY")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("010-0000-4001")))
                .andExpect(content().string(containsString("ceo@mobility.example.com")))
                .andExpect(content().string(not(containsString("{ARIA}"))));
        mockMvc.perform(get("/mypage")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("평가위원 화면에는 사업자등록번호를 가린 값만 내려가고, 담당자는 전체를 본다")
    void businessNoMaskedForEvaluator() throws Exception {
        for (String path : new String[]{"/applications", "/applications/2", "/evaluator/evaluations/3"}) {
            mockMvc.perform(get(path).with(user("evaluator1").roles("EVALUATOR")))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("504-86-*****")))
                    .andExpect(content().string(not(containsString("504-86-54322"))));
        }
        mockMvc.perform(get("/applications/2").with(user("manager1").roles("MANAGER")))
                .andExpect(content().string(containsString("504-86-54322")));
    }
}
