package kr.go.daegu.pms.notice.service;

import lombok.Data;

/**
 * 공지 게시판의 첨부 규칙. 전자정부 공통컴포넌트 게시판 속성(COMTNBBSMASTER)에서 읽는다.
 * 코드가 아니라 이 값(Flyway V3 에서 설정)으로 첨부 허용 여부·개수·크기가 정해진다.
 */
@Data
public class NoticeAttachPolicyVO {

    /** 첨부 가능 여부 (FILE_ATCH_POSBL_AT, Y/N) */
    private String fileAtchPosblAt;
    /** 게시물 하나에 올릴 수 있는 파일 수 (ATCH_POSBL_FILE_NUMBER) */
    private int posblAtchFileNumber;
    /** 파일 하나의 최대 크기, 바이트 (ATCH_POSBL_FILE_SIZE). 0 이면 게시판 한도 없음 → 서버 전체 업로드 한도만 적용 */
    private long posblAtchFileSize;

    public boolean isAllowed() {
        return "Y".equals(fileAtchPosblAt) && posblAtchFileNumber > 0;
    }

    /** 검사에 쓸 파일당 최대 크기 (바이트) */
    public long getMaxFileSize() {
        return posblAtchFileSize > 0 ? posblAtchFileSize : Long.MAX_VALUE;
    }

    /** 화면 안내용 MB 단위 (게시판 한도가 없으면 0) */
    public long getMaxFileSizeMb() {
        return posblAtchFileSize / 1024 / 1024;
    }
}
