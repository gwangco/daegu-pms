package kr.go.daegu.pms;

import kr.go.daegu.pms.notice.service.NoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공지사항 첨부파일 (8차).
 * 공통컴포넌트 게시판(COMTNBBS)의 첨부 ID 에 첨부파일 묶음(COMTNFILE/COMTNFILEDETAIL)을 연결하고,
 * 개수·크기는 게시판 속성(COMTNBBSMASTER)으로 서버에서 검사한다. 공지 첨부는 비로그인도 받을 수 있다.
 */
class NoticeAttachmentIntegrationTest extends IntegrationTestSupport {

    private static final RequestPostProcessor MANAGER = user("manager1").roles("MANAGER");

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("게시판 속성(Flyway V3): 공지 게시판은 첨부 가능, 공지 하나에 3개, 파일당 10MB")
    void boardPolicyComesFromMigration() {
        Map<String, Object> board = jdbc.queryForMap(
                "SELECT FILE_ATCH_POSBL_AT, ATCH_POSBL_FILE_NUMBER, ATCH_POSBL_FILE_SIZE FROM COMTNBBSMASTER WHERE BBS_ID = ?",
                NoticeService.BBS_ID);
        assertThat(board.get("FILE_ATCH_POSBL_AT")).isEqualTo("Y");
        assertThat(((Number) board.get("ATCH_POSBL_FILE_NUMBER")).intValue()).isEqualTo(3);
        assertThat(((Number) board.get("ATCH_POSBL_FILE_SIZE")).longValue()).isEqualTo(10L * 1024 * 1024);
    }

    @Test
    @DisplayName("등록: 파일 2개가 묶음 하나(순번 0, 1)로 저장되고, 게시물에는 서버가 만든 첨부 ID 가 연결된다 (화면이 보낸 첨부 ID 는 무시)")
    void createWithFiles() throws Exception {
        long nttId = create(MANAGER, "첨부 공지", pdf("files", "공고문.pdf"), pdf("files", "신청서식.pdf"));

        String atchFileId = atchFileId(nttId);
        assertThat(atchFileId).matches("FILE_\\d{15}").isNotEqualTo("FILE_000000000000999");
        assertThat(fileNames(atchFileId)).containsExactly("0:공고문.pdf", "1:신청서식.pdf");

        mockMvc.perform(get("/notices/" + nttId))
                .andExpect(content().string(containsString("공고문.pdf")))
                .andExpect(content().string(containsString("/notices/" + nttId + "/files/1")));
        mockMvc.perform(get("/notices"))
                .andExpect(content().string(containsString("첨부파일 있음")));
    }

    @Test
    @DisplayName("공지 첨부는 비로그인도 받는다. 없는 순번·첨부 없는 공지·삭제된 공지는 404, 업무 첨부 주소(/files)로는 내주지 않는다")
    void anonymousDownload() throws Exception {
        long nttId = create(MANAGER, "누구나 받는 첨부", pdf("files", "안내문.pdf"));
        String atchFileId = atchFileId(nttId);

        mockMvc.perform(get("/notices/" + nttId + "/files/0"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename*=UTF-8''")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(PDF_BYTES));

        mockMvc.perform(get("/notices/" + nttId + "/files/9")).andExpect(status().isNotFound());
        mockMvc.perform(get("/notices/1/files/0")).andExpect(status().isNotFound());

        // 업무 첨부 주소는 로그인이 필요하고, 로그인해도 공지 첨부는 업무 데이터가 아니라 404
        mockMvc.perform(get("/files/" + atchFileId + "/0")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/files/" + atchFileId + "/0").with(user("company1").roles("COMPANY")))
                .andExpect(status().isNotFound());

        // 공지를 지우면(논리 삭제) 첨부도 내려받을 수 없다
        mockMvc.perform(post("/notices/" + nttId + "/delete").with(MANAGER).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/notices/" + nttId + "/files/0")).andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("게시판 속성의 개수(3개)를 넘으면 글도 파일도 저장하지 않고, 입력한 제목은 그대로 둔 채 안내한다")
    void tooManyFiles() throws Exception {
        int notices = countNotices();
        int files = countFileDetails();

        mockMvc.perform(withFiles(multipart("/notices/new"), pdf("files", "1.pdf"), pdf("files", "2.pdf"),
                                pdf("files", "3.pdf"), pdf("files", "4.pdf"))
                        .param("nttSj", "파일이 많은 공지").param("nttCn", "본문").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3개까지 올릴 수 있습니다")))
                .andExpect(content().string(containsString("파일이 많은 공지")));

        assertThat(countNotices()).isEqualTo(notices);
        assertThat(countFileDetails()).isEqualTo(files);
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("파일당 크기(10MB)를 넘거나, 내용이 확장자와 다르면 거부한다")
    void sizeAndSignatureAreChecked() throws Exception {
        byte[] big = Arrays.copyOf(PDF_BYTES, 10 * 1024 * 1024 + 1);
        mockMvc.perform(withFiles(multipart("/notices/new"), new MockMultipartFile("files", "큰파일.pdf", "application/pdf", big))
                        .param("nttSj", "큰 파일").param("nttCn", "본문").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("파일당 10MB 까지")))
                .andExpect(content().string(containsString("큰파일.pdf")));

        MockMultipartFile fake = new MockMultipartFile("files", "가짜.pdf", "application/pdf",
                "<html>script</html>".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(withFiles(multipart("/notices/new"), fake)
                        .param("nttSj", "가짜 파일").param("nttCn", "본문").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("확장자(.pdf)와 일치하지 않습니다")));
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("수정: 지울 파일 표시 + 새 파일 추가. 새 파일은 마지막 순번 다음, 다른 공지의 순번·첨부 ID 를 보내도 건드리지 않는다")
    void updateFiles() throws Exception {
        long nttId = create(null, "수정할 공지", pdf("files", "a.pdf"), pdf("files", "b.pdf"));
        long otherId = create(null, "다른 공지", pdf("files", "other.pdf"));
        String atchFileId = atchFileId(nttId);
        String otherAtchFileId = atchFileId(otherId);

        mockMvc.perform(withFiles(multipart("/notices/" + nttId + "/edit"), pdf("files", "c.pdf"))
                        .param("nttSj", "수정할 공지").param("nttCn", "바뀐 본문")
                        .param("deleteFileSn", "1").param("deleteFileSn", "7")
                        .param("atchFileId", otherAtchFileId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // b.pdf(1) 삭제, c.pdf 는 지우기 전 마지막 순번(1) 다음인 2 → 예전 주소 /files/1 이 다른 파일을 가리키지 않는다
        assertThat(atchFileId(nttId)).isEqualTo(atchFileId);
        assertThat(fileNames(atchFileId)).containsExactly("0:a.pdf", "2:c.pdf");
        assertThat(fileNames(otherAtchFileId)).containsExactly("0:other.pdf");
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("수정에서 남은 파일 + 새 파일이 3개를 넘으면 거부, 삭제 표시로 자리를 만들면 허용")
    void countIncludesExistingFiles() throws Exception {
        long nttId = create(null, "꽉 찬 공지", pdf("files", "1.pdf"), pdf("files", "2.pdf"), pdf("files", "3.pdf"));

        mockMvc.perform(withFiles(multipart("/notices/" + nttId + "/edit"), pdf("files", "4.pdf"))
                        .param("nttSj", "꽉 찬 공지").param("nttCn", "본문").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("3개까지 올릴 수 있습니다")));

        mockMvc.perform(withFiles(multipart("/notices/" + nttId + "/edit"), pdf("files", "4.pdf"))
                        .param("nttSj", "꽉 찬 공지").param("nttCn", "본문").param("deleteFileSn", "0").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(fileNames(atchFileId(nttId))).containsExactly("1:2.pdf", "2:3.pdf", "3:4.pdf");
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("첨부 삭제는 DB 커밋 뒤에 실제 파일을 지운다 (이 테스트는 롤백되므로 요청이 끝나도 파일이 남아 있어야 한다)")
    void physicalFileIsDeletedOnlyAfterCommit() throws Exception {
        long nttId = create(null, "삭제 시점 확인", pdf("files", "keep.pdf"));
        String atchFileId = atchFileId(nttId);
        Path stored = storedPath(atchFileId, 0);
        assertThat(stored).exists();

        mockMvc.perform(multipart("/notices/" + nttId + "/edit")
                        .param("nttSj", "삭제 시점 확인").param("nttCn", "본문").param("deleteFileSn", "0").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(fileNames(atchFileId)).isEmpty();
        assertThat(stored).exists();   // 공통컴포넌트 원본은 여기서 이미 지워져, 롤백되면 DB 와 실제 파일이 어긋났다
        // 파일이 하나도 없는 묶음은 목록에 첨부 표시를 하지 않는다
        mockMvc.perform(get("/notices").param("searchCnd", "0").param("searchWrd", "삭제 시점 확인"))
                .andExpect(content().string(not(containsString("첨부파일 있음"))));
    }

    @Test
    @WithUserDetails("manager1")
    @DisplayName("게시판 속성에서 첨부를 막으면(N) 업로드를 거부한다. 첨부 없는 공지는 그대로 쓸 수 있다")
    void attachmentsCanBeTurnedOffByBoardSetting() throws Exception {
        jdbc.update("UPDATE COMTNBBSMASTER SET FILE_ATCH_POSBL_AT = 'N' WHERE BBS_ID = ?", NoticeService.BBS_ID);
        int notices = countNotices();

        mockMvc.perform(withFiles(multipart("/notices/new"), pdf("files", "a.pdf"))
                        .param("nttSj", "첨부 금지").param("nttCn", "본문").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("첨부파일을 받지 않습니다")));
        assertThat(countNotices()).isEqualTo(notices);

        create(null, "첨부 없는 공지");
        assertThat(countNotices()).isEqualTo(notices + 1);
        mockMvc.perform(get("/notices/new"))
                .andExpect(content().string(not(containsString("name=\"files\""))));
    }

    @Test
    @DisplayName("기업회원은 첨부를 포함해 공지를 쓸 수 없다")
    void companyCannotUpload() throws Exception {
        mockMvc.perform(withFiles(multipart("/notices/new"), pdf("files", "a.pdf"))
                        .param("nttSj", "기업 공지").param("nttCn", "본문")
                        .with(user("company1").roles("COMPANY")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    /** 공지를 등록하고 게시물 번호를 돌려준다 (who 가 null 이면 테스트의 @WithUserDetails 사용자) */
    private long create(RequestPostProcessor who, String title, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = withFiles(multipart("/notices/new"), files);
        request.param("nttSj", title).param("nttCn", "본문").param("atchFileId", "FILE_000000000000999").with(csrf());
        if (who != null) {
            request.with(who);
        }
        String location = mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        return Long.parseLong(location.replace("/notices/", ""));
    }

    private static MockMultipartHttpServletRequestBuilder withFiles(MockMultipartHttpServletRequestBuilder request,
                                                                    MockMultipartFile... files) {
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        return request;
    }

    private String atchFileId(long nttId) {
        return jdbc.queryForObject("SELECT ATCH_FILE_ID FROM COMTNBBS WHERE NTT_ID = ?", String.class, nttId);
    }

    /** "순번:원래 이름" 목록 (순번 순) */
    private List<String> fileNames(String atchFileId) {
        return jdbc.queryForList("SELECT FILE_SN, ORIGNL_FILE_NM FROM COMTNFILEDETAIL WHERE ATCH_FILE_ID = ? ORDER BY FILE_SN",
                        atchFileId).stream()
                .map(row -> ((Number) row.get("FILE_SN")).intValue() + ":" + row.get("ORIGNL_FILE_NM"))
                .toList();
    }

    private Path storedPath(String atchFileId, int fileSn) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT FILE_STRE_COURS, STRE_FILE_NM FROM COMTNFILEDETAIL WHERE ATCH_FILE_ID = ? AND FILE_SN = ?", atchFileId, fileSn);
        return Paths.get("build/test-uploads").resolve((String) row.get("FILE_STRE_COURS")).resolve((String) row.get("STRE_FILE_NM"));
    }

    private int countNotices() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM COMTNBBS WHERE BBS_ID = ?", Integer.class, NoticeService.BBS_ID);
        return count == null ? 0 : count;
    }

    private int countFileDetails() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM COMTNFILEDETAIL", Integer.class);
        return count == null ? 0 : count;
    }
}
