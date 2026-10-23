package kr.go.daegu.pms.cmm.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 개인정보 처리 기록 한 건 (누가, 언제, 어디서, 누구의 정보를, 무엇을 했나) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacyAccessLogVO {

    public static final String ACTION_VIEW = "VIEW";
    public static final String ACTION_DOWNLOAD = "DOWNLOAD";

    public static final String TARGET_APPLICATION = "APPLICATION";
    public static final String TARGET_USER_LIST = "USER_LIST";
    public static final String TARGET_EXCEL = "EXCEL";
    public static final String TARGET_FILE = "FILE";

    private Long logId;
    private String userId;
    private String userName;      // 조인용
    private String action;
    private String targetType;
    private String targetId;
    private String subjectId;
    private int recordCount;
    private String clientIp;
    private LocalDateTime accessedAt;

    public String getActionLabel() {
        return ACTION_DOWNLOAD.equals(action) ? "다운로드" : "조회";
    }

    public String getTargetLabel() {
        if (targetType == null) {
            return "";
        }
        return switch (targetType) {
            case TARGET_APPLICATION -> "신청서 상세";
            case TARGET_USER_LIST -> "사용자 목록";
            case TARGET_EXCEL -> "신청 현황 엑셀";
            case TARGET_FILE -> "첨부파일";
            default -> targetType;
        };
    }
}
