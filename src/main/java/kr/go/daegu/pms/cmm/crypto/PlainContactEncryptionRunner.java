package kr.go.daegu.pms.cmm.crypto;

import kr.go.daegu.pms.user.service.UserManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 기동할 때 평문으로 남은 연락처·이메일을 암호문으로 바꾼다 (app.crypto.encrypt-plain-on-startup=true 일 때만).
 *
 * 로컬 데모는 data.sql 의 seed 가 평문이라 켜 둔다. 운영에서 기존 데이터를 전환할 때는
 * 백업 → 별도 전환 작업 → 건수 대사(전환 전후 비교) 순서로 진행하고 이 설정은 끈다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.crypto", name = "encrypt-plain-on-startup", havingValue = "true")
public class PlainContactEncryptionRunner implements ApplicationRunner {

    private final UserManageService userManageService;

    @Override
    public void run(ApplicationArguments args) {
        userManageService.updatePlainContactsToEncrypted();
    }
}
