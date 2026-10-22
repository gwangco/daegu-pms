package kr.go.daegu.pms;

import kr.go.daegu.pms.announcement.service.AnnouncementService;
import kr.go.daegu.pms.announcement.service.AnnouncementVO;
import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.cmm.exception.NotFoundException;
import kr.go.daegu.pms.cmm.message.AppMessages;
import kr.go.daegu.pms.settlement.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 메시지 분리 후에도 실제 안내 문구·숫자 형식·예외 종류·HTTP 처리가 유지되는지 검증한다. */
class MessageIntegrationTest extends IntegrationTestSupport {

    @Autowired AppMessages messages;
    @Autowired MessageSource messageSource;
    @Autowired AnnouncementService announcementService;
    @Autowired SettlementService settlementService;

    @Test
    @DisplayName("모든 사용 메시지 키가 번들에 존재하며 누락 키는 코드 문자열로 대체하지 않는다")
    void allReferencedKeysExistAndMissingKeyFails() throws Exception {
        Pattern reference = Pattern.compile("messages\\.(?:text|business|notFound|denied)\\(\\s*\"([^\"]+)\"");
        Set<String> referenced = new HashSet<>();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                var matcher = reference.matcher(Files.readString(path, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    referenced.add(matcher.group(1));
                }
            }
        }
        assertThat(referenced).contains("application.period.closed", "settlement.balance.exceeded", "file.name.invalid");
        for (String code : referenced) {
            assertThatCode(() -> messageSource.getMessage(code, null, Locale.KOREA))
                    .as("번들 누락 키: %s", code).doesNotThrowAnyException();
        }
        assertThatThrownBy(() -> messages.text("missing.message.for.test"))
                .isInstanceOf(NoSuchMessageException.class);
    }

    @Test
    @DisplayName("메시지 키는 호출 위치에 문자열로 적는다 (변수로 넘기면 위의 누락 키 검사가 찾지 못한다)")
    void messageKeysAreWrittenAtCallSite() throws Exception {
        Pattern indirect = Pattern.compile("messages\\.(?:text|business|notFound|denied)\\(\\s*[^\"\\s]");
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertThat(indirect.matcher(Files.readString(path, StandardCharsets.UTF_8)).find())
                        .as("메시지 키를 변수로 전달: %s", path).isFalse();
            }
        }
    }

    @Test
    @DisplayName("한글·날짜·원화 금액·길이·작은따옴표 인자를 정확히 치환한다")
    void formatsArgumentsWithoutLosingKoreanTextOrQuotes() {
        assertThat(messages.text("application.period.closed", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)))
                .isEqualTo("접수 기간이 아닌 공고입니다. (접수기간: 2026-10-01 ~ 2026-10-20)");
        assertThat(messages.text("application.grant.exceeded", 50000000L))
                .isEqualTo("신청 지원금이 공고의 기업당 최대 지원금(50,000,000원)을 초과합니다.");
        assertThat(messages.text("settlement.balance.exceeded", 1234567L, 2000000L))
                .isEqualTo("협약 지원금 잔액을 초과합니다. (등록 가능 잔액: 1,234,567원 / 이번 집행액: 2,000,000원)");
        assertThat(messages.text("application.status.transitionDenied", "접수완료", "최종선정(협약)"))
                .isEqualTo("'접수완료' 상태에서 '최종선정(협약)' 상태로 변경할 수 없습니다.");
        assertThat(messages.text("evaluation.comment.tooLong", 2000))
                .isEqualTo("종합 의견은 2000자 이내로 입력하세요.");
        assertThat(messages.text("file.signature.invalid", "pdf"))
                .isEqualTo("파일 내용이 확장자(.pdf)와 일치하지 않습니다. 올바른 파일을 첨부하세요.");
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    @DisplayName("JVM·요청 언어가 달라도 한국어 서비스의 원화 표기를 유지한다")
    void koreanFormattingIsIndependentOfJvmAndRequestLocale() {
        Locale oldDefault = Locale.getDefault();
        LocaleContext oldContext = LocaleContextHolder.getLocaleContext();
        try {
            Locale.setDefault(Locale.GERMANY);
            LocaleContextHolder.setLocale(Locale.FRANCE);
            assertThat(messages.text("application.grant.exceeded", 1234567L))
                    .isEqualTo("신청 지원금이 공고의 기업당 최대 지원금(1,234,567원)을 초과합니다.");
        } finally {
            Locale.setDefault(oldDefault);
            LocaleContextHolder.setLocaleContext(oldContext);
        }
    }

    @Test
    @WithMockUser(username = "company1", roles = "COMPANY")
    @DisplayName("AOP 서비스 경계에서도 업무·데이터없음·권한 런타임 예외 분류를 유지한다")
    void preservesRuntimeExceptionCategoriesThroughServiceProxy() {
        assertThatThrownBy(() -> settlementService.selectSettlementApplication(3L))
                .isExactlyInstanceOf(BusinessException.class)
                .hasMessage("최종 선정(협약)된 과제만 사업비 정산을 이용할 수 있습니다.");
        assertThatThrownBy(() -> announcementService.selectAnnouncement(-1L))
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 공고입니다.");
        assertThatThrownBy(() -> announcementService.insertAnnouncement(new AnnouncementVO()))
                .isExactlyInstanceOf(AccessDeniedException.class)
                .hasMessage("공고는 사업담당자만 등록할 수 있습니다.");
    }

    @Test
    @WithMockUser(username = "company1", roles = "COMPANY")
    @DisplayName("메시지 외부화 후 업무 400·데이터없음 404·권한 403 응답을 유지한다")
    void preservesHttpErrorResponses() throws Exception {
        mockMvc.perform(get("/settlements/application/3"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attribute("errorMessage", "최종 선정(협약)된 과제만 사업비 정산을 이용할 수 있습니다."));
        mockMvc.perform(get("/announcements/999999"))
                .andExpect(status().isNotFound())
                .andExpect(model().attribute("errorMessage", "존재하지 않는 공고입니다."));
        mockMvc.perform(get("/announcements/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager1", roles = "MANAGER")
    @DisplayName("상태 전이 오류의 작은따옴표와 인자가 리다이렉트 안내에도 유지된다")
    void controllerFlashContainsResolvedMessage() throws Exception {
        mockMvc.perform(post("/applications/3/status").with(csrf()).param("status", "SELECTED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "'접수완료' 상태에서 '최종선정(협약)' 상태로 변경할 수 없습니다."));
    }
}
