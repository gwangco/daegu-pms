package kr.go.daegu.pms.service.impl;

import kr.go.daegu.pms.common.exception.BusinessException;
import kr.go.daegu.pms.common.exception.NotFoundException;
import kr.go.daegu.pms.common.util.SecurityUtil;
import kr.go.daegu.pms.config.AppProperties;
import kr.go.daegu.pms.domain.AttachFile;
import kr.go.daegu.pms.mapper.FileMapper;
import kr.go.daegu.pms.service.ApplicationAccessChecker;
import kr.go.daegu.pms.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 첨부파일 처리 구현체.
 *
 * 시큐어코딩 - 위험한 형식 파일 업로드 / 경로 조작 방지:
 *  1) 확장자 화이트리스트 검사 (application.yml app.file.allowed-extensions)
 *  2) 파일 시그니처(매직넘버) 검사 → "webshell.jsp" 를 "plan.pdf" 로 이름만 바꾼 파일 차단
 *  3) 서버 저장명은 UUID(확장자 없음) → 업로드 파일이 서버에서 실행될 여지 제거
 *  4) 저장/다운로드 경로가 업로드 루트 밖으로 벗어나지 않는지 normalize 후 검사 (../ 경로 조작 차단)
 *  5) 다운로드 시 파일이 연결된 과제의 열람 권한을 재검사 (파일번호 변조 IDOR 차단)
 */
@Slf4j
@Service("fileService")
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int MAX_ORIGINAL_NAME_LENGTH = 200;
    private static final DateTimeFormatter SUB_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    /** 확장자별 파일 시그니처(파일 첫 바이트). 하나라도 일치하면 정상 파일로 판단 */
    private static final Map<String, List<String>> FILE_SIGNATURES = Map.ofEntries(
            Map.entry("pdf", List.of("25504446")),               // %PDF
            Map.entry("png", List.of("89504e470d0a1a0a")),
            Map.entry("jpg", List.of("ffd8ff")),
            Map.entry("jpeg", List.of("ffd8ff")),
            Map.entry("zip", List.of("504b0304")),               // ZIP 기반
            Map.entry("docx", List.of("504b0304")),
            Map.entry("xlsx", List.of("504b0304")),
            Map.entry("hwpx", List.of("504b0304")),
            Map.entry("hwp", List.of("d0cf11e0a1b11ae1")),       // MS 복합문서(OLE) 기반
            Map.entry("doc", List.of("d0cf11e0a1b11ae1")),
            Map.entry("xls", List.of("d0cf11e0a1b11ae1"))
    );

    private final FileMapper fileMapper;
    private final ApplicationAccessChecker accessChecker;
    private final AppProperties appProperties;
    private final Clock clock;

    @Override
    @Transactional
    public AttachFile storeFile(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("첨부파일을 선택하세요.");
        }
        String userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }

        String originalName = sanitizeFileName(multipartFile.getOriginalFilename());
        String ext = extractExtension(originalName);
        List<String> allowed = appProperties.file().allowedExtensions();
        if (!allowed.contains(ext)) {
            throw new BusinessException("허용되지 않는 파일 형식입니다. (허용 형식: " + String.join(", ", allowed) + ")");
        }
        verifySignature(multipartFile, ext);

        String subDir = YearMonth.now(clock).format(SUB_DIR_FORMAT);
        String storedName = UUID.randomUUID().toString().replace("-", "");
        Path target = resolveSafely(subDir, storedName);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = multipartFile.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("첨부파일 저장 중 오류가 발생했습니다.", e);
        }
        deleteOnRollback(target);

        AttachFile file = AttachFile.builder()
                .originalName(originalName)
                .storedName(storedName)
                .filePath(subDir)
                .fileExt(ext)
                .fileSize(multipartFile.getSize())
                .createdBy(userId)
                .build();
        fileMapper.insertFile(file);
        log.info("[파일 업로드] fileId={}, user={}, size={}", file.getFileId(), userId, file.getFileSize());
        return file;
    }

    @Override
    @Transactional(readOnly = true)
    public Download loadDownload(Long fileId) {
        AttachFile file = fileMapper.selectFile(fileId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 파일입니다."));

        // 파일이 어느 과제에 속하는지 찾아 그 과제의 접근 권한으로 다운로드 권한을 판단
        Long planOwnerApplicationId = fileMapper.selectApplicationIdByPlanFile(fileId);
        Long receiptOwnerApplicationId = fileMapper.selectApplicationIdByReceiptFile(fileId);
        if (planOwnerApplicationId != null) {
            accessChecker.getReadableApplication(planOwnerApplicationId);
        } else if (receiptOwnerApplicationId != null) {
            accessChecker.getSettlementAccessibleApplication(receiptOwnerApplicationId);
        } else {
            // 어떤 업무 데이터에도 연결되지 않은 파일은 노출하지 않는다
            throw new NotFoundException("존재하지 않는 파일입니다.");
        }

        Path path = resolveSafely(file.getFilePath(), file.getStoredName());
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException("서버에 파일이 존재하지 않습니다.");
        }
        return new Download(file, new FileSystemResource(path));
    }

    /** 경로 구분자/제어문자 제거 및 길이 제한. 원본 파일명은 표시용으로만 쓰고 저장 경로에는 절대 사용하지 않는다 */
    static String sanitizeFileName(String originalFilename) {
        if (originalFilename == null) {
            throw new BusinessException("파일명이 올바르지 않습니다.");
        }
        // 일부 브라우저는 "C:\Users\...\plan.pdf" 처럼 전체 경로를 보내므로 마지막 이름만 사용
        String name = originalFilename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        // 응답 헤더 분할(CRLF) 등에 악용될 수 있는 제어문자 제거
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            throw new BusinessException("파일명이 올바르지 않습니다.");
        }
        if (name.length() > MAX_ORIGINAL_NAME_LENGTH) {
            String ext = extractExtension(name);
            name = (ext.isEmpty() || ext.length() > 10)
                    ? name.substring(0, MAX_ORIGINAL_NAME_LENGTH)
                    : name.substring(0, MAX_ORIGINAL_NAME_LENGTH - ext.length() - 1) + "." + ext;
        }
        return name;
    }

    static String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void verifySignature(MultipartFile multipartFile, String ext) {
        List<String> signatures = FILE_SIGNATURES.get(ext);
        if (signatures == null) {
            return;
        }
        byte[] header = new byte[8];
        int read;
        try (InputStream in = multipartFile.getInputStream()) {
            read = in.readNBytes(header, 0, header.length);
        } catch (IOException e) {
            throw new UncheckedIOException("첨부파일을 읽는 중 오류가 발생했습니다.", e);
        }
        String headerHex = HexFormat.of().formatHex(Arrays.copyOf(header, read));
        boolean matched = signatures.stream().anyMatch(headerHex::startsWith);
        if (!matched) {
            log.warn("[파일 업로드 차단] 확장자와 실제 파일 형식 불일치 ext={}, user={}", ext, SecurityUtil.getCurrentUserId());
            throw new BusinessException("파일 내용이 확장자(." + ext + ")와 일치하지 않습니다. 올바른 파일을 첨부하세요.");
        }
    }

    /** 업로드 루트 기준으로 경로를 만들고, 루트 밖으로 벗어나면 차단 */
    private Path resolveSafely(String subDir, String storedName) {
        Path root = Paths.get(appProperties.file().uploadDir()).toAbsolutePath().normalize();
        Path target = root.resolve(subDir).resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("잘못된 파일 경로입니다.");
        }
        return target;
    }

    /** DB 저장이 실패해 트랜잭션이 롤백되면 이미 디스크에 쓴 파일도 삭제 (고아 파일 방지) */
    private void deleteOnRollback(Path target) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException e) {
                        log.warn("[파일 정리 실패] {}", target, e);
                    }
                }
            }
        });
    }
}
