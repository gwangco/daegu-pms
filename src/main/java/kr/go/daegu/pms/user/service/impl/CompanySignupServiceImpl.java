package kr.go.daegu.pms.user.service.impl;

import kr.go.daegu.pms.cmm.crypto.PersonalInfoCipher;
import kr.go.daegu.pms.cmm.exception.FieldBusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.cmm.util.BusinessNoUtil;
import kr.go.daegu.pms.cmm.util.PhoneNoUtil;
import kr.go.daegu.pms.cmm.util.SecurityUtil;
import kr.go.daegu.pms.user.service.CompanySignupService;
import kr.go.daegu.pms.user.service.CompanySignupVO;
import kr.go.daegu.pms.user.service.PasswordPolicy;
import kr.go.daegu.pms.user.service.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 기업회원 가입.
 *
 * 입력 규칙은 모두 여기서 확인하고, 어긴 칸을 한 번에 모아 FieldBusinessException 으로 돌려준다.
 * 권한(ROLE_COMPANY)과 승인 대기(REQUESTED)는 화면 값이 아니라 INSERT SQL 에 고정돼 있다.
 * 비밀번호는 BCrypt, 연락처·이메일은 eGov ARIA 로 저장한다 (기존 사용자와 같은 방식).
 */
@Slf4j
@Service("companySignupService")
@RequiredArgsConstructor
public class CompanySignupServiceImpl extends EgovAbstractServiceImpl implements CompanySignupService {

    /** 영문 소문자로 시작하는 영문 소문자·숫자 4~20자 */
    private static final Pattern USER_ID = Pattern.compile("[a-z][a-z0-9]{3,19}");

    /** 관리자·담당자·평가위원 계정과 헷갈리는 아이디는 기업회원이 쓰지 못하게 한다 */
    static final List<String> RESERVED_ID_PREFIXES = List.of("admin", "manager", "evaluator", "system", "dpms");

    /** 암호문이 컬럼(VARCHAR 255)에 들어가도록 영문 주소만, 100자까지 */
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_COMPANY_LENGTH = 100;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PersonalInfoCipher cipher;
    private final AppMessages messages;

    @Override
    public String insertCompanyMember(CompanySignupVO signup) {
        if (SecurityUtil.getCurrentUserId() != null) {
            throw messages.business("signup.loggedIn");
        }
        Map<String, String> errors = new LinkedHashMap<>();

        String userId = trim(signup.getUserId());
        if (userId == null || !USER_ID.matcher(userId).matches()) {
            errors.put("userId", messages.text("signup.userId.invalid"));
        } else if (RESERVED_ID_PREFIXES.stream().anyMatch(userId::startsWith)) {
            errors.put("userId", messages.text("signup.userId.reserved"));
        } else if (userMapper.selectUser(userId).isPresent()) {
            // 아이디가 있는지 알려 주게 되지만, 가입 화면에서는 피할 수 없는 정보라 안내를 우선한다
            errors.put("userId", messages.text("signup.userId.duplicate"));
        }

        PasswordPolicy.check(signup.getNewPassword(), userId).ifPresentOrElse(
                violation -> errors.put("newPassword", passwordMessage(violation)),
                () -> {
                    if (!Objects.equals(signup.getNewPassword(), signup.getNewPasswordConfirm())) {
                        errors.put("newPasswordConfirm", messages.text("signup.password.mismatch"));
                    }
                });

        String userName = trim(signup.getUserName());
        if (userName == null || userName.length() > MAX_NAME_LENGTH) {
            errors.put("userName", messages.text("signup.userName.invalid"));
        }
        String companyName = trim(signup.getCompanyName());
        if (companyName == null || companyName.length() > MAX_COMPANY_LENGTH) {
            errors.put("companyName", messages.text("signup.companyName.invalid"));
        }

        String businessNo = BusinessNoUtil.normalize(signup.getBusinessNo());
        if (businessNo == null) {
            errors.put("businessNo", messages.text("signup.businessNo.format"));
        } else if (!BusinessNoUtil.isValid(businessNo)) {
            errors.put("businessNo", messages.text("signup.businessNo.checksum"));
        } else if (userMapper.selectCompanyBusinessNoCnt(businessNo) > 0) {
            errors.put("businessNo", messages.text("signup.businessNo.duplicate"));
        }

        String email = trim(signup.getEmail());
        if (email == null || email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
            errors.put("email", messages.text("signup.email.invalid"));
        }
        String phone = PhoneNoUtil.normalize(signup.getPhone());
        if (phone == null) {
            errors.put("phone", messages.text("signup.phone.invalid"));
        }

        if (!signup.isAgreeTerms()) {
            errors.put("agreeTerms", messages.text("signup.consent.terms"));
        }
        if (!signup.isAgreePrivacy()) {
            errors.put("agreePrivacy", messages.text("signup.consent.privacy"));
        }
        if (!errors.isEmpty()) {
            throw new FieldBusinessException(errors);
        }

        UserVO member = UserVO.builder()
                .userId(userId)
                .password(passwordEncoder.encode(signup.getNewPassword()))
                .userName(userName)
                .email(cipher.encrypt(email))
                .phone(cipher.encrypt(phone))
                .companyName(companyName)
                .businessNo(businessNo)
                .build();
        try {
            userMapper.insertCompanyMember(member);
        } catch (DuplicateKeyException e) {
            // 위에서 확인한 뒤 저장하기 전에 같은 아이디(기본키)나 사업자등록번호(UNIQUE)로 다른 가입이 먼저 저장된 경우
            throw messages.business("signup.duplicate.race");
        }
        log.info("[기업회원 가입 신청] user={}", userId);
        return userId;
    }

    private String passwordMessage(PasswordPolicy.Violation violation) {
        return switch (violation) {
            case CHARACTERS -> messages.text("signup.password.characters");
            case LENGTH -> messages.text("signup.password.length", PasswordPolicy.MIN_LENGTH_THREE_KINDS,
                    PasswordPolicy.MIN_LENGTH_TWO_KINDS, PasswordPolicy.MAX_LENGTH);
            case CONTAINS_USER_ID -> messages.text("signup.password.userId");
            case REPEATED -> messages.text("signup.password.repeated");
            case SEQUENTIAL -> messages.text("signup.password.sequential");
        };
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
