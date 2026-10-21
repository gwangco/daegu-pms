package kr.go.daegu.pms.application.service.impl;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import kr.go.daegu.pms.application.service.ApplicationVO;
import kr.go.daegu.pms.application.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Apache POI 엑셀 생성.
 * ※ 셀 값은 setCellValue(String) 으로 "문자열"로만 넣는다. "=CMD()" 같은 값이 들어와도 수식으로 실행되지 않는다.
 *   (CSV 로 내보낼 때는 =, +, -, @ 로 시작하는 값을 이스케이프해야 하는 CSV 인젝션에 주의)
 */
@Service("excelService")
public class ExcelServiceImpl extends EgovAbstractServiceImpl implements ExcelService {

    @Override
    public byte[] exportApplicationsToExcel(List<ApplicationVO> applications) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("지원사업_신청_및_선정현황");

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            setBorders(headerStyle);

            // 데이터 스타일
            CellStyle dataStyle = workbook.createCellStyle();
            setBorders(dataStyle);

            DataFormat format = workbook.createDataFormat();
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(format.getFormat("#,##0"));
            setBorders(numberStyle);

            CellStyle scoreStyle = workbook.createCellStyle();
            scoreStyle.setDataFormat(format.getFormat("0.0"));
            setBorders(scoreStyle);

            // 헤더 행 생성
            String[] headers = {"접수번호", "사업공고명", "기업명", "사업자번호", "과제명", "총예산(원)", "신청지원금(원)", "기업부담금(원)", "진행상태", "평균평가점수", "접수일시"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 행 삽입
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (ApplicationVO app : applications) {
                Row row = sheet.createRow(rowIdx++);

                Cell c0 = row.createCell(0); c0.setCellValue(app.getReceiptNo()); c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(app.getAnnouncementTitle()); c1.setCellStyle(dataStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(app.getCompanyName()); c2.setCellStyle(dataStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(app.getBusinessNo()); c3.setCellStyle(dataStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(app.getProjectName()); c4.setCellStyle(dataStyle);

                Cell c5 = row.createCell(5); c5.setCellValue(app.getTotalBudget()); c5.setCellStyle(numberStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(app.getRequestGrant()); c6.setCellStyle(numberStyle);
                Cell c7 = row.createCell(7); c7.setCellValue(app.getSelfFunding()); c7.setCellStyle(numberStyle);

                Cell c8 = row.createCell(8); c8.setCellValue(app.getStatusName() != null ? app.getStatusName() : app.getStatus()); c8.setCellStyle(dataStyle);
                Cell c9 = row.createCell(9);
                if (app.getAvgScore() != null) {
                    c9.setCellValue(app.getAvgScore());
                    c9.setCellStyle(scoreStyle);
                } else {
                    c9.setCellValue("-");
                    c9.setCellStyle(dataStyle);
                }
                Cell c10 = row.createCell(10); c10.setCellValue(app.getSubmittedAt() != null ? app.getSubmittedAt().format(formatter) : ""); c10.setCellStyle(dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // 체크 예외를 런타임 예외로 감싼다 (eGov 예외 후처리 AOP 가 선언되지 않은 체크 예외를 던지지 않도록)
            throw new UncheckedIOException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
