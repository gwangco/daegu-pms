package kr.go.daegu.pms;

import kr.go.daegu.pms.cmm.exception.BusinessException;
import kr.go.daegu.pms.settlement.service.SettlementService;
import kr.go.daegu.pms.settlement.service.SettlementVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithUserDetails("company1")
class SettlementAmountSecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired SettlementService settlementService;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("최대 long 공급가액으로 부가세 합계를 음수로 만들어 예산 검사를 우회할 수 없다")
    void overflowCannotBypassBudgetCheck() throws Exception {
        int settlementsBefore = count("settlement");
        int filesBefore = count("COMTNFILE");   // 공통컴포넌트 첨부파일 테이블

        mockMvc.perform(multipart("/settlements/register")
                        .file(pdf("receiptFile", "overflow.pdf")).with(csrf())
                        .param("applicationId", "1").param("itemCode", "EXP02")
                        .param("expenseDate", "2026-10-20").param("vendorName", "테스트 거래처")
                        .param("description", "오버플로 공격 검증")
                        .param("supplyAmount", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settlements/application/1"))
                .andExpect(flash().attribute("errorMessage", containsString("처리 가능한 범위")));

        assertThat(count("settlement")).isEqualTo(settlementsBefore);
        assertThat(count("COMTNFILE")).isEqualTo(filesBefore);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    @DisplayName("컨트롤러를 거치지 않아도 null·0·음수 공급가액은 서비스에서 거부한다")
    void serviceRejectsInvalidSupply(Long supply) {
        SettlementVO expense = SettlementVO.builder()
                .applicationId(1L).itemCode("EXP02").expenseDate(LocalDate.of(2026, 10, 20))
                .vendorName("테스트 거래처").description("금액 검사").supplyAmount(supply).build();
        int before = count("settlement");
        assertThatThrownBy(() -> settlementService.insertSettlement(expense, pdf("receiptFile", "amount.pdf")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("0원보다");
        assertThat(count("settlement")).isEqualTo(before);
    }

    private int count(String table) {
        // 테이블명은 위 테스트의 고정 상수만 사용한다.
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
