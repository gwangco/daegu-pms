package kr.go.daegu.pms.cmm.service;

import java.util.Locale;

/**
 * 화면에 보여 줄 첨부파일 한 건.
 * 공통컴포넌트 FileVO 에서 표시에 필요한 값만 옮기고, 서버 저장 경로·저장 파일명은 담지 않는다.
 *
 * @param fileSn    묶음 안의 순번 (COMTNFILEDETAIL.FILE_SN)
 * @param name      올릴 때의 파일 이름
 * @param extension 확장자 (소문자)
 * @param size      바이트
 */
public record AttachedFile(int fileSn, String name, String extension, long size) {

    /** 1.2MB, 340KB 처럼 읽기 쉬운 크기 */
    public String sizeLabel() {
        if (size >= 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1fMB", size / 1024.0 / 1024.0);
        }
        return Math.max(1, Math.round(size / 1024.0)) + "KB";
    }
}
