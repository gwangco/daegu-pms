package kr.go.daegu.pms.application.service;


import java.util.List;

public interface ExcelService {

    byte[] exportApplicationsToExcel(List<ApplicationVO> applications);
}
