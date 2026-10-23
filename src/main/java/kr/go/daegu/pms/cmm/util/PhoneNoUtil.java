package kr.go.daegu.pms.cmm.util;

/**
 * 국내 전화번호를 하이픈 형식으로 정리한다.
 * 저장 형식을 하나로 맞춰 두면 화면 마스킹(MaskingUtil.phone)과 검색이 같은 기준으로 동작한다.
 */
public final class PhoneNoUtil {

    private PhoneNoUtil() {
    }

    /**
     * 숫자만 남겨 국번 규칙에 맞게 하이픈을 넣는다. 국내 번호 형식이 아니면 null.
     * 01012345678 → 010-1234-5678, 0212345678 → 02-1234-5678, 053-741-1234 → 053-741-1234
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("[\\s-]", "");
        if (!digits.matches("0\\d{8,10}")) {
            return null;
        }
        if (digits.startsWith("02")) {
            // 서울 02: 9자리(02-123-4567) 또는 10자리(02-1234-5678)
            return switch (digits.length()) {
                case 9 -> digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
                case 10 -> digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
                default -> null;
            };
        }
        // 휴대전화·지역번호 3자리: 10자리(053-741-1234) 또는 11자리(010-1234-5678)
        return switch (digits.length()) {
            case 10 -> digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
            case 11 -> digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
            default -> null;
        };
    }
}
