package kr.go.daegu.pms.service.impl;

import kr.go.daegu.pms.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileNameSanitizeTest {

    @Test
    @DisplayName("경로가 포함된 파일명은 마지막 이름만 남긴다 (경로 조작 차단)")
    void stripPath() {
        assertThat(FileServiceImpl.sanitizeFileName("../../etc/passwd.pdf")).isEqualTo("passwd.pdf");
        assertThat(FileServiceImpl.sanitizeFileName("C:\\Users\\me\\사업계획서.pdf")).isEqualTo("사업계획서.pdf");
    }

    @Test
    @DisplayName("CRLF 등 제어문자를 제거한다 (응답 헤더 분할 차단)")
    void stripControlChars() {
        assertThat(FileServiceImpl.sanitizeFileName("plan\r\nSet-Cookie: x.pdf")).isEqualTo("planSet-Cookie: x.pdf");
    }

    @Test
    @DisplayName("비어 있거나 상위경로 표기만 있는 파일명은 거부한다")
    void rejectInvalid() {
        assertThatThrownBy(() -> FileServiceImpl.sanitizeFileName("..")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> FileServiceImpl.sanitizeFileName("   ")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> FileServiceImpl.sanitizeFileName(null)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("확장자는 마지막 점 기준, 소문자로 추출한다 (이중 확장자 webshell.jsp.pdf → pdf, plan.PDF.jsp → jsp)")
    void extension() {
        assertThat(FileServiceImpl.extractExtension("Plan.PDF")).isEqualTo("pdf");
        assertThat(FileServiceImpl.extractExtension("plan.PDF.jsp")).isEqualTo("jsp");
        assertThat(FileServiceImpl.extractExtension("noext")).isEmpty();
        assertThat(FileServiceImpl.extractExtension("dot.")).isEmpty();
    }
}
