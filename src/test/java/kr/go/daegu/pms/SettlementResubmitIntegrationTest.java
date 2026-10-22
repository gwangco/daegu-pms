package kr.go.daegu.pms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 정산: 반려 건 보완 후 재요청, 처리 이력, 목록 페이징, 담당자 검수함 */
class SettlementResubmitIntegrationTest extends IntegrationTestSupport {

    @Autowired JdbcTemplate jdbc;

    /** seed 정산 2(검수요청 24,200,000원)를 담당자가 반려한다 */
    private void rejectSeedSettlement() throws Exception {
        mockMvc.perform(post("/settlements/2/review").with(csrf()).with(user("manager1").roles("MANAGER"))
                        .param("applicationId", "1").param("status", "REJECTED").param("rejectReason", "세금계산서 공급가액 불일치"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    private MockMultipartHttpServletRequestBuilder resubmitRequest(long settlementId, String supplyAmount) {
        return (MockMultipartHttpServletRequestBuilder) multipart("/settlements/" + settlementId + "/resubmit")
                .param("applicationId", "1").param("itemCode", "EXP03").param("expenseDate", "2026-10-19")
                .param("vendorName", "북성로정밀금형").param("supplyAmount", supplyAmount)
                .param("description", "케이스 가공비 (세금계산서 재발행)")
                .with(csrf());
    }

    private ResultActions resubmitAs(String userId, long settlementId, String supplyAmount) throws Exception {
        return mockMvc.perform(resubmitRequest(settlementId, supplyAmount).with(user(userId).roles("COMPANY")));
    }

    private List<String> historyActions(long settlementId) {
        return jdbc.queryForList("SELECT action_code FROM settlement_history WHERE settlement_id = ? ORDER BY history_id",
                String.class, settlementId);
    }

    @Test
    @DisplayName("반려 건을 고쳐 재요청하면 금액을 서버에서 다시 계산하고 검수요청으로 돌아가며, 반려 사유는 이력에 남는다")
    void resubmitRejectedSettlement() throws Exception {
        rejectSeedSettlement();

        resubmitAs("company1", 2, "20000000")
                .andExpect(redirectedUrl("/settlements/application/1"))
                .andExpect(flash().attribute("successMessage", "보완한 내용으로 다시 검수를 요청했습니다."));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT status, reject_reason, reviewed_by, resubmit_count, item_code, vat_amount, total_amount, updated_by
                FROM settlement WHERE settlement_id = 2""");
        assertThat(row.get("status")).isEqualTo("REQUESTED");
        assertThat(row.get("reject_reason")).isNull();
        assertThat(row.get("reviewed_by")).isNull();
        assertThat(row.get("resubmit_count")).isEqualTo(1);
        assertThat(row.get("item_code")).isEqualTo("EXP03");
        assertThat(row.get("vat_amount")).isEqualTo(2_000_000L);
        assertThat(row.get("total_amount")).isEqualTo(22_000_000L);
        assertThat(row.get("updated_by")).isEqualTo("company1");

        assertThat(historyActions(2)).containsExactly("REQUESTED", "REJECTED", "RESUBMITTED");
        assertThat(jdbc.queryForObject("SELECT memo FROM settlement_history WHERE settlement_id = 2 AND action_code = 'REJECTED'", String.class))
                .isEqualTo("세금계산서 공급가액 불일치");

        // 화면의 처리 이력에 반려 사유와 재요청이 보인다
        mockMvc.perform(get("/settlements/application/1").with(user("company1").roles("COMPANY")))
                .andExpect(content().string(containsString("사유: 세금계산서 공급가액 불일치")))
                .andExpect(content().string(containsString("보완 후 재요청")));
    }

    @Test
    @DisplayName("새 증빙을 올리면 교체하고, 다른 기업은 재요청할 수 없다")
    void newReceiptAndOwnership() throws Exception {
        rejectSeedSettlement();
        resubmitAs("company2", 2, "1000").andExpect(status().isForbidden());

        mockMvc.perform(resubmitRequest(2, "1000").file(pdf("receiptFile", "재발행_세금계산서.pdf"))
                        .with(user("company1").roles("COMPANY")))
                .andExpect(flash().attributeExists("successMessage"));
        String atchFileId = jdbc.queryForObject("SELECT receipt_atch_file_id FROM settlement WHERE settlement_id = 2", String.class);
        assertThat(atchFileId).matches("FILE_\\d{15}");
        mockMvc.perform(get("/files/" + atchFileId + "/0").with(user("company1").roles("COMPANY"))).andExpect(status().isOk());
        mockMvc.perform(get("/files/" + atchFileId + "/0").with(user("company2").roles("COMPANY"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("반려되지 않은 건, 이미 재요청한 건, 잔액을 넘는 금액은 거부하고 상태를 바꾸지 않는다")
    void resubmitRules() throws Exception {
        // 검수요청 상태(반려 전)는 재요청 불가
        resubmitAs("company1", 2, "1000")
                .andExpect(flash().attribute("errorMessage", "보완 요청(반려)된 정산 건만 다시 요청할 수 있습니다."));

        rejectSeedSettlement();
        // 잔액 = 1억 - 승인 1,650만 = 8,350만. 공급가 8,000만(+부가세 800만) = 8,800만 → 초과
        resubmitAs("company1", 2, "80000000")
                .andExpect(flash().attribute("errorMessage", containsString("잔액을 초과")));
        assertThat(jdbc.queryForObject("SELECT status FROM settlement WHERE settlement_id = 2", String.class)).isEqualTo("REJECTED");

        resubmitAs("company1", 2, "1000").andExpect(flash().attributeExists("successMessage"));
        resubmitAs("company1", 2, "2000")
                .andExpect(flash().attribute("errorMessage", "보완 요청(반려)된 정산 건만 다시 요청할 수 있습니다."));
        assertThat(jdbc.queryForObject("SELECT resubmit_count FROM settlement WHERE settlement_id = 2", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("정산 목록은 eGov 페이징(10건)과 상태 필터를 쓰고, 다른 과제 ID 를 파라미터로 넣어도 무시한다")
    void settlementPaging() throws Exception {
        for (int i = 1; i <= 11; i++) {
            jdbc.update("""
                    INSERT INTO settlement (application_id, item_code, expense_date, vendor_name, supply_amount, vat_amount,
                        total_amount, description, status, created_by)
                    VALUES (1, 'EXP01', '2026-10-01', ?, 100, 10, 110, '페이징 확인', 'APPROVED', 'company1')
                    """, "거래처" + i);
        }
        mockMvc.perform(get("/settlements/application/1").param("applicationId", "999").with(user("company1").roles("COMPANY")))
                .andExpect(model().attribute("settlements", hasSize(10)))
                .andExpect(content().string(containsString("data-page=\"2\"")));
        mockMvc.perform(get("/settlements/application/1").param("pageIndex", "2").with(user("company1").roles("COMPANY")))
                .andExpect(model().attribute("settlements", hasSize(3)));
        mockMvc.perform(get("/settlements/application/1").param("status", "REQUESTED").with(user("company1").roles("COMPANY")))
                .andExpect(model().attribute("settlements", hasSize(1)));
    }

    @Test
    @DisplayName("담당자 검수함은 전체 과제의 검수 요청을 보여 주고, 기업·평가위원은 들어올 수 없다")
    void reviewQueue() throws Exception {
        mockMvc.perform(get("/settlements/review").with(user("manager1").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("settlements", hasSize(1)))
                .andExpect(content().string(containsString("DPMS-00000001")))
                .andExpect(content().string(containsString("북성로정밀금형")));
        mockMvc.perform(get("/settlements/review").param("status", "").with(user("manager1").roles("MANAGER")))
                .andExpect(model().attribute("settlements", hasSize(2)));
        mockMvc.perform(get("/settlements/review").with(user("company1").roles("COMPANY"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/settlements/review").with(user("evaluator1").roles("EVALUATOR"))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("새로 등록한 정산도 등록·검수 이력이 남는다")
    void historyForNewSettlement() throws Exception {
        mockMvc.perform(multipart("/settlements/register").file(pdf("receiptFile", "h.pdf"))
                        .with(csrf()).with(user("company1").roles("COMPANY"))
                        .param("applicationId", "1").param("itemCode", "EXP04").param("expenseDate", "2026-10-20")
                        .param("vendorName", "이력 거래처").param("supplyAmount", "500000").param("description", "출장 여비"))
                .andExpect(flash().attributeExists("successMessage"));
        Long id = jdbc.queryForObject("SELECT settlement_id FROM settlement WHERE vendor_name = '이력 거래처'", Long.class);
        mockMvc.perform(post("/settlements/" + id + "/review").with(csrf()).with(user("manager1").roles("MANAGER"))
                .param("applicationId", "1").param("status", "APPROVED"));

        assertThat(historyActions(id)).containsExactly("REQUESTED", "APPROVED");
        assertThat(jdbc.queryForList("SELECT actor_id FROM settlement_history WHERE settlement_id = ? ORDER BY history_id", String.class, id))
                .containsExactly("company1", "manager1");
    }
}
