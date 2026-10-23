package kr.go.daegu.pms.cmm.util;

/**
 * 사업자등록번호 형식 정리와 검증번호 확인.
 *
 * 사업자등록번호 10자리 = 세무서 코드 3자리 + 개인·법인 구분 2자리 + 일련번호 4자리 + 검증번호 1자리.
 * 마지막 검증번호는 앞 9자리로 계산되므로, 숫자 하나를 잘못 친 번호는 대부분 여기서 걸러진다.
 * 실제로 등록된 사업자인지(휴·폐업 포함)는 국세청 사업자 상태 조회 같은 외부 연계가 필요해 여기서는 확인하지 않는다.
 */
public final class BusinessNoUtil {

    /** 앞 9자리에 곱하는 가중치 */
    private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    private BusinessNoUtil() {
    }

    /**
     * 하이픈·공백을 빼고 숫자 10자리면 000-00-00000 형식으로 돌려준다. 그 밖의 입력은 null.
     * 1234567891, 123 45 67891, 123-45-67891 → 123-45-67891
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("[\\s-]", "");
        if (!digits.matches("\\d{10}")) {
            return null;
        }
        return digits.substring(0, 3) + "-" + digits.substring(3, 5) + "-" + digits.substring(5);
    }

    /**
     * 검증번호가 맞는지 확인한다. 쓰지 않는 코드(세무서 000, 구분 00)도 거부한다.
     * 계산: 앞 9자리 × 가중치(1,3,7,1,3,7,1,3,5)의 합 + (9번째 자리 × 5 ÷ 10 의 몫) → 10 에서 합의 끝자리를 뺀 값의 끝자리
     */
    public static boolean isValid(String businessNo) {
        String normalized = normalize(businessNo);
        if (normalized == null) {
            return false;
        }
        String digits = normalized.replace("-", "");
        if (digits.startsWith("000") || digits.startsWith("00", 3)) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        sum += (digits.charAt(8) - '0') * 5 / 10;
        int checkDigit = (10 - sum % 10) % 10;
        return checkDigit == digits.charAt(9) - '0';
    }
}
