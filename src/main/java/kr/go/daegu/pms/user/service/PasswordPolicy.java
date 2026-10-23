package kr.go.daegu.pms.user.service;

import java.util.Locale;
import java.util.Optional;

/**
 * 비밀번호 규칙.
 *
 * 조합·길이는 KISA 「패스워드 선택 및 이용 안내서」의 기준을 따른다.
 *  - 영문 대문자·영문 소문자·숫자·특수문자 중 3종류 이상이면 8자 이상, 2종류면 10자 이상
 *  - 최대 64자. BCrypt 는 앞 72바이트만 쓰므로 그보다 짧게 두고, 한글·공백은 받지 않아 한 글자가 1바이트가 되게 한다
 * 추측하기 쉬운 비밀번호를 줄이려고 다음도 막는다.
 *  - 아이디가 들어간 비밀번호
 *  - 같은 문자 3번 연속 (aaa, 111)
 *  - 연속된 문자 4자 (1234, abcd, 4321, dcba)
 *
 * 가입과 (다음에 만들) 비밀번호 변경이 같은 규칙을 쓰도록 한 곳에 둔다.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH_THREE_KINDS = 8;
    public static final int MIN_LENGTH_TWO_KINDS = 10;
    public static final int MAX_LENGTH = 64;

    /** 어긴 규칙. 여러 개를 어겨도 위에서부터 하나만 알려 준다 */
    public enum Violation {
        /** 공백·한글·제어문자 등 키보드의 영문·숫자·특수문자가 아닌 글자 */
        CHARACTERS,
        /** 조합 종류에 비해 짧거나 최대 길이를 넘음 */
        LENGTH,
        CONTAINS_USER_ID,
        REPEATED,
        SEQUENTIAL
    }

    private PasswordPolicy() {
    }

    /** 규칙을 모두 지키면 빈 값, 아니면 처음 어긴 규칙 */
    public static Optional<Violation> check(String password, String userId) {
        if (password == null || password.isEmpty() || !password.chars().allMatch(PasswordPolicy::isAllowed)) {
            return Optional.of(Violation.CHARACTERS);
        }
        int kinds = kinds(password);
        int length = password.length();
        boolean longEnough = (kinds >= 3 && length >= MIN_LENGTH_THREE_KINDS)
                || (kinds == 2 && length >= MIN_LENGTH_TWO_KINDS);
        if (!longEnough || length > MAX_LENGTH) {
            return Optional.of(Violation.LENGTH);
        }
        if (userId != null && !userId.isBlank()
                && password.toLowerCase(Locale.ROOT).contains(userId.trim().toLowerCase(Locale.ROOT))) {
            return Optional.of(Violation.CONTAINS_USER_ID);
        }
        if (hasRepeated(password)) {
            return Optional.of(Violation.REPEATED);
        }
        if (hasSequence(password)) {
            return Optional.of(Violation.SEQUENTIAL);
        }
        return Optional.empty();
    }

    /** 키보드의 영문·숫자·특수문자 (ASCII 0x21~0x7E, 공백 제외) */
    private static boolean isAllowed(int c) {
        return c >= 0x21 && c <= 0x7E;
    }

    private static int kinds(String password) {
        boolean upper = password.chars().anyMatch(c -> c >= 'A' && c <= 'Z');
        boolean lower = password.chars().anyMatch(c -> c >= 'a' && c <= 'z');
        boolean digit = password.chars().anyMatch(c -> c >= '0' && c <= '9');
        boolean special = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        return (upper ? 1 : 0) + (lower ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
    }

    private static boolean hasRepeated(String password) {
        for (int i = 2; i < password.length(); i++) {
            char c = password.charAt(i);
            if (c == password.charAt(i - 1) && c == password.charAt(i - 2)) {
                return true;
            }
        }
        return false;
    }

    /** 숫자끼리 또는 영문끼리(대소문자 무시) 1씩 오르거나 내리는 4자 */
    private static boolean hasSequence(String password) {
        String lower = password.toLowerCase(Locale.ROOT);
        for (int i = 0; i + 3 < lower.length(); i++) {
            if (isRun(lower, i, 1) || isRun(lower, i, -1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRun(String s, int start, int step) {
        char first = s.charAt(start);
        boolean digit = Character.isDigit(first);
        boolean letter = first >= 'a' && first <= 'z';
        if (!digit && !letter) {
            return false;
        }
        for (int k = 1; k < 4; k++) {
            char c = s.charAt(start + k);
            boolean sameKind = digit ? Character.isDigit(c) : (c >= 'a' && c <= 'z');
            if (!sameKind || c != first + step * k) {
                return false;
            }
        }
        return true;
    }
}
