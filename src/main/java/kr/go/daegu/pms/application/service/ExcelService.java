package kr.go.daegu.pms.application.service;


import java.io.IOException;
import java.util.List;

public interface ExcelService {

    byte[] exportApplicationsToExcel(List<ApplicationVO> applications) throws IOException;
}
