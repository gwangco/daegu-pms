package kr.go.daegu.pms.cmm.crypto;

import org.egovframe.boot.crypto.EgovCryptoProperties;
import org.egovframe.boot.crypto.EgovPasswordEncoder;
import org.egovframe.boot.crypto.service.EgovARIACryptoService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 개인정보 컬럼(연락처·이메일) 암·복호화.
 *
 * 전자정부 암호화 서비스(egovframe-boot-starter-crypto)의 EgovARIACryptoService 를 쓰고,
 * DB 에는 "{ARIA}" + Base64 형태로 저장한다. 앞에 붙인 표식으로 평문/암호문을 구분하므로
 * 기존 평문 데이터를 단계적으로 전환할 수 있고, 나중에 알고리즘을 바꿀 때도 표식으로 구분할 수 있다.
 *
 * 확인한 eGov ARIA 구현 특성 (PersonalInfoCryptoIntegrationTest 에 회귀 테스트로 남김)
 *  - 같은 평문은 항상 같은 암호문이 된다 (IV 없이 블록 단위 처리). 같은 값끼리 비교는 되지만 값의 빈도가 드러날 수 있다
 *  - 평문 길이가 16바이트 배수이고 끝이 패딩처럼 보이는 바이트(0x01 등)면 복호화 결과가 잘린다
 *    → 텍스트 개인정보에는 그런 제어문자가 없으므로 제어문자가 들어간 값은 암호화하지 않고 거부한다
 *  - 키 검증: 설정한 키의 SHA-256 해시가 algorithm-key-hash 와 다르면 암·복호화를 거부한다
 */
@Component
public class PersonalInfoCipher {

    public static final String PREFIX = "{ARIA}";

    /** ARIA-256 마스터 키 길이. eGov ARIACipher 는 32자를 넘으면 잘라 쓰고 모자라면 패딩하므로 정확히 32자를 요구한다 */
    private static final int KEY_LENGTH = 32;

    private final EgovARIACryptoService ariaCryptoService;
    private final String key;

    public PersonalInfoCipher(EgovARIACryptoService ariaCryptoService,
                              EgovPasswordEncoder egovPasswordEncoder,
                              EgovCryptoProperties cryptoProperties) {
        String configuredKey = cryptoProperties.getAlgorithmKey();
        if (configuredKey == null || configuredKey.length() != KEY_LENGTH || !configuredKey.matches("[A-Za-z0-9]+")) {
            throw new IllegalStateException("개인정보 암호화 키(org.egovframe.crypto.algorithm-key)는 영문·숫자 32자여야 합니다. "
                    + "운영 환경은 DPMS_CRYPTO_KEY, DPMS_CRYPTO_KEY_HASH 환경변수로 지정하세요.");
        }
        if (cryptoProperties.getAlgorithmKeyHash() == null || !egovPasswordEncoder.checkPassword(configuredKey)) {
            throw new IllegalStateException("암호화 키와 키 해시(org.egovframe.crypto.algorithm-key-hash)가 일치하지 않습니다.");
        }
        this.ariaCryptoService = ariaCryptoService;
        this.key = configuredKey;
    }

    /** 평문 → {ARIA}Base64. null·빈 값과 이미 암호화된 값은 그대로 둔다 */
    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty() || isEncrypted(plain)) {
            return plain;
        }
        if (plain.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("제어문자가 포함된 값은 암호화하지 않습니다.");
        }
        byte[] encrypted = ariaCryptoService.encrypt(plain.getBytes(StandardCharsets.UTF_8), key);
        return PREFIX + Base64.getEncoder().encodeToString(encrypted);
    }

    /** {ARIA}Base64 → 평문. 표식이 없는 값(전환 전 평문)은 그대로 돌려준다 */
    public String decrypt(String stored) {
        if (!isEncrypted(stored)) {
            return stored;
        }
        byte[] encrypted = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        return new String(ariaCryptoService.decrypt(encrypted, key), StandardCharsets.UTF_8);
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
