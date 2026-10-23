package kr.go.daegu.pms;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SXSSF 로 만든 엑셀의 실제 내용 검증 */
class ExcelExportIntegrationTest extends IntegrationTestSupport {

    @Autowired JdbcTemplate jdbc;

    private XSSFWorkbook download(String query) throws Exception {
        byte[] bytes = mockMvc.perform(get("/excel/applications" + query).with(user("manager1").roles("MANAGER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    @DisplayName("머리글과 전체 신청 건이 들어가고 금액은 숫자 셀이다")
    void contents() throws Exception {
        try (XSSFWorkbook workbook = download("")) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("지원사업_신청_및_선정현황");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("접수번호");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();   // 머리글 행 고정
            assertThat(sheet.getLastRowNum()).isEqualTo(3);   // seed 신청 3건

            Row first = sheet.getRow(3);   // 최신순 정렬이므로 마지막 행이 DPMS-00000001
            assertThat(first.getCell(0).getStringCellValue()).isEqualTo("DPMS-00000001");
            assertThat(first.getCell(3).getStringCellValue()).isEqualTo("504-81-98761");   // 담당자 엑셀은 원본
            assertThat(first.getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(first.getCell(5).getNumericCellValue()).isEqualTo(125_000_000d);
        }
    }

    @Test
    @DisplayName("수식처럼 보이는 입력도 문자열 셀로 저장되어 엑셀에서 실행되지 않는다")
    void formulaLikeInputStaysText() throws Exception {
        jdbc.update("""
                INSERT INTO application (receipt_no, announcement_id, company_user_id, project_name, total_budget,
                    request_grant, self_funding, summary, status)
                VALUES ('DPMS-EXCEL-1', 16, 'company2', '=HYPERLINK("http://example.com","확인")', 1000, 500, 500, 's', 'SUBMITTED')
                """);
        try (XSSFWorkbook workbook = download("?searchKeyword=DPMS-EXCEL-1")) {
            Cell projectName = workbook.getSheetAt(0).getRow(1).getCell(4);
            assertThat(projectName.getCellType()).isEqualTo(CellType.STRING);
            assertThat(projectName.getStringCellValue()).startsWith("=HYPERLINK");
        }
    }
}
