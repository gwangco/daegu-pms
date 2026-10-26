package kr.go.daegu.pms;

import kr.go.daegu.pms.notice.service.NoticeService;
import kr.go.daegu.pms.notice.service.NoticeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공지 동시 수정 (8차 후속). 담당자와 관리자가 같은 공지에 동시에 파일을 올리면, 뒤 요청은 게시물 행 잠금(SELECT … FOR UPDATE)을
 * 기다렸다가 앞 요청이 커밋한 첨부 묶음과 순번을 보고 이어서 붙인다. 순번이 겹치거나 첨부 묶음이 둘로 갈라지지 않아야 한다.
 * 앞 요청은 트랜잭션 안에서 실행하고 커밋을 미뤄 경쟁을 만든다. 전용 공지를 만들고 지우므로 시드 데이터는 바뀌지 않는다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@WithMockUser(username = "manager1", roles = "MANAGER")
class NoticeConcurrencyIntegrationTest extends IntegrationTestSupport {

    private static final Path UPLOAD_ROOT = Path.of("build/test-uploads");

    @Autowired NoticeService noticeService;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private ExecutorService executor;
    private Long nttId;

    private NoticeVO notice(Long id) {
        NoticeVO notice = new NoticeVO();
        if (id != null) {
            notice.setNttId(id);
        }
        notice.setNttSj("동시 수정 확인용 공지");
        notice.setNttCn("담당자 두 명이 동시에 첨부를 올리는 경우");
        notice.setNoticeAt(NoticeVO.NO);
        return notice;
    }

    @BeforeEach
    void createDedicatedNotice() {
        executor = Executors.newSingleThreadExecutor();
        nttId = noticeService.insertNotice(notice(null), List.of());
    }

    @AfterEach
    void removeDedicatedNotice() throws Exception {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        String atchFileId = jdbc.queryForObject("SELECT ATCH_FILE_ID FROM COMTNBBS WHERE NTT_ID = ?", String.class, nttId);
        if (atchFileId != null) {
            for (Map<String, Object> file : jdbc.queryForList(
                    "SELECT FILE_STRE_COURS, STRE_FILE_NM FROM COMTNFILEDETAIL WHERE ATCH_FILE_ID = ?", atchFileId)) {
                Files.deleteIfExists(UPLOAD_ROOT.resolve((String) file.get("FILE_STRE_COURS")).resolve((String) file.get("STRE_FILE_NM")));
            }
            jdbc.update("DELETE FROM COMTNFILEDETAIL WHERE ATCH_FILE_ID = ?", atchFileId);
            jdbc.update("DELETE FROM COMTNFILE WHERE ATCH_FILE_ID = ?", atchFileId);
        }
        jdbc.update("DELETE FROM COMTNBBS WHERE NTT_ID = ?", nttId);
    }

    @Test
    @DisplayName("같은 공지에 동시에 파일을 올리면 뒤 요청은 행 잠금을 기다렸다가 같은 첨부 묶음에 다음 순번으로 붙인다")
    void concurrentUploadsShareOneGroup() throws Exception {
        int groupsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM COMTNFILE", Integer.class);
        AtomicBoolean waitedOnRowLock = new AtomicBoolean();

        Future<Throwable> second = new TransactionTemplate(txManager).execute(status -> {
            noticeService.updateNotice(notice(nttId), List.of(pdf("files", "first-upload.pdf")), List.of());
            Future<Throwable> future = DatabaseLockWait.submitAs(executor, "admin", "ROLE_ADMIN",
                    () -> noticeService.updateNotice(notice(nttId), List.of(pdf("files", "second-upload.pdf")), List.of()));
            waitedOnRowLock.set(DatabaseLockWait.awaitLockWaitOrCompletion(jdbc, future));
            return future;
        });

        assertThat(second).isNotNull();
        assertThat(second.get(15, TimeUnit.SECONDS)).as("뒤 요청도 오류 없이 끝남").isNull();
        assertThat(waitedOnRowLock).as("뒤 요청이 앞 요청의 게시물 행 잠금을 실제로 기다림").isTrue();

        String atchFileId = jdbc.queryForObject("SELECT ATCH_FILE_ID FROM COMTNBBS WHERE NTT_ID = ?", String.class, nttId);
        List<Map<String, Object>> files = jdbc.queryForList(
                "SELECT FILE_SN, ORIGNL_FILE_NM FROM COMTNFILEDETAIL WHERE ATCH_FILE_ID = ? ORDER BY FILE_SN", atchFileId);
        assertThat(files).extracting(f -> ((Number) f.get("FILE_SN")).intValue()).containsExactly(0, 1);
        assertThat(files).extracting(f -> f.get("ORIGNL_FILE_NM")).containsExactly("first-upload.pdf", "second-upload.pdf");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM COMTNFILE", Integer.class))
                .as("뒤 요청이 첨부 묶음을 따로 만들지 않음").isEqualTo(groupsBefore + 1);
    }
}
