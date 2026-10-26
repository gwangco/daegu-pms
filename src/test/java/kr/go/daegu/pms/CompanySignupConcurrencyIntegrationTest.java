package kr.go.daegu.pms;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.user.service.CompanySignupService;
import kr.go.daegu.pms.user.service.CompanySignupVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 가입 경쟁 (9차 후속). 서비스의 중복 확인을 둘 다 통과한 가입이 겹쳐 저장되면 DB 의 기본키·UNIQUE 가 늦은 쪽을 막고,
 * 서비스는 그 DB 중복 오류를 화면용 안내 문구로 바꾼다.
 * 앞 가입은 트랜잭션 안에서 저장만 하고 커밋을 미뤄, 뒤 가입의 사전 확인이 앞 가입을 보지 못하는 순간을 만든다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CompanySignupConcurrencyIntegrationTest extends IntegrationTestSupport {

    /** 검증번호가 맞는 가상의 번호 (시드에 없는 번호) */
    private static final String BUSINESS_NO_A = "220-81-12341";
    private static final String BUSINESS_NO_B = "314-86-20200";

    @Autowired CompanySignupService signupService;
    @Autowired AppMessages messages;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private ExecutorService executor;

    private static CompanySignupVO signup(String userId, String businessNo) {
        CompanySignupVO signup = new CompanySignupVO();
        signup.setUserId(userId);
        signup.setNewPassword("Dpms!2026");
        signup.setNewPasswordConfirm("Dpms!2026");
        signup.setUserName("홍담당");
        signup.setCompanyName("(주)동시가입테스트");
        signup.setBusinessNo(businessNo);
        signup.setEmail("race@newcorp.example.com");
        signup.setPhone("01098765432");
        signup.setAgreeTerms(true);
        signup.setAgreePrivacy(true);
        return signup;
    }

    @BeforeEach
    void startExecutor() {
        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void removeRaceMembers() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        jdbc.update("DELETE FROM users WHERE user_id IN ('racecorp1', 'racecorp2')");
    }

    @Test
    @DisplayName("같은 사업자등록번호로 동시에 가입하면 늦은 쪽은 DB 오류 대신 다시 확인하라는 안내를 받고, 앞 가입만 남는다")
    void sameBusinessNoAtOnce() throws Exception {
        Throwable late = race(signup("racecorp1", BUSINESS_NO_A), signup("racecorp2", BUSINESS_NO_A));

        assertThat(late).isInstanceOf(BusinessException.class).hasMessage(messages.text("signup.duplicate.race"));
        assertThat(jdbc.queryForList("SELECT user_id FROM users WHERE business_no = ?", String.class, BUSINESS_NO_A))
                .containsExactly("racecorp1");
    }

    @Test
    @DisplayName("같은 아이디로 동시에 가입해도 같은 안내를 받고, 먼저 저장된 가입의 정보가 바뀌지 않는다")
    void sameUserIdAtOnce() throws Exception {
        Throwable late = race(signup("racecorp1", BUSINESS_NO_A), signup("racecorp1", BUSINESS_NO_B));

        assertThat(late).isInstanceOf(BusinessException.class).hasMessage(messages.text("signup.duplicate.race"));
        assertThat(jdbc.queryForObject("SELECT business_no FROM users WHERE user_id = 'racecorp1'", String.class))
                .isEqualTo(BUSINESS_NO_A);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE business_no = ?", Integer.class, BUSINESS_NO_B)).isZero();
    }

    /** 앞 가입을 커밋하지 않은 채 뒤 가입을 다른 스레드에서 시작하고, 뒤 가입이 DB 잠금을 기다리는 것을 확인한 뒤 커밋한다 */
    private Throwable race(CompanySignupVO first, CompanySignupVO second) throws Exception {
        AtomicBoolean waitedOnDuplicateKey = new AtomicBoolean();
        Future<Throwable> late = new TransactionTemplate(txManager).execute(status -> {
            signupService.insertCompanyMember(first);
            Future<Throwable> future = DatabaseLockWait.submitAs(executor, null, null,
                    () -> signupService.insertCompanyMember(second));
            waitedOnDuplicateKey.set(DatabaseLockWait.awaitLockWaitOrCompletion(jdbc, future));
            return future;
        });
        assertThat(late).isNotNull();
        Throwable outcome = late.get(15, TimeUnit.SECONDS);
        assertThat(waitedOnDuplicateKey).as("뒤 가입이 사전 확인을 통과하고 저장에서 앞 가입의 키 잠금을 기다림").isTrue();
        return outcome;
    }
}
