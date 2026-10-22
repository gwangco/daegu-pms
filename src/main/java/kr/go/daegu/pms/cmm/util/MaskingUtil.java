package kr.go.daegu.pms.cmm.util;

/**
 * 개인정보 화면 표시용 마스킹.
 *
 * 화면(HTML)에서 CSS 나 스크립트로 가리면 페이지 소스에 원본이 남는다.
 * 그래서 서버에서 마스킹한 값을 모델에 넣고, 원본이 필요 없는 사용자에게는 원본을 보내지 않는다.
 * 가리는 위치는 개인정보 비식별 조치 가이드에서 흔히 쓰는 방식(이름 가운데, 전화번호 가운데 국번 등)을 따랐다.
 */
public final class MaskingUtil {

    private static final char MASK = '*';

    private MaskingUtil() {
    }

    /** 010-1234-5678 → 010-****-5678, 0537401234 → 053***1234 */
    public static String phone(String value) {
        if (isBlank(value)) {
            return value;
        }
        String[] parts = value.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + repeat(parts[1].length()) + "-" + parts[2];
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return repeat(value.length());
        }
        int keepTail = digits.length() >= 10 ? 4 : 3;
        return digits.substring(0, 3) + repeat(digits.length() - 3 - keepTail) + digits.substring(digits.length() - keepTail);
    }

    /** ceo@mobility.example.com → ce*@mobility.example.com (아이디 앞 2자리만 남김) */
    public static String email(String value) {
        if (isBlank(value)) {
            return value;
        }
        int at = value.indexOf('@');
        if (at <= 0) {
            return repeat(value.length());
        }
        String local = value.substring(0, at);
        int keep = local.length() <= 2 ? 1 : 2;
        return local.substring(0, keep) + repeat(local.length() - keep) + value.substring(at);
    }

    /** 504-81-98765 → 504-81-*****, 5048198765 → 50481***** */
    public static String businessNo(String value) {
        if (isBlank(value)) {
            return value;
        }
        String[] parts = value.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + parts[1] + "-" + repeat(parts[2].length());
        }
        int keep = Math.min(5, value.length() / 2);
        return value.substring(0, keep) + repeat(value.length() - keep);
    }

    /** 홍길동 → 홍*동, 홍길 → 홍*, 남궁민수 → 남**수 */
    public static String name(String value) {
        if (isBlank(value) || value.length() == 1) {
            return value == null ? null : repeat(value.length());
        }
        if (value.length() == 2) {
            return value.charAt(0) + String.valueOf(MASK);
        }
        return value.charAt(0) + repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String repeat(int count) {
        return String.valueOf(MASK).repeat(Math.max(0, count));
    }
}
