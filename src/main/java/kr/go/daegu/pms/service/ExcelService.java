package kr.go.daegu.pms.service;

import kr.go.daegu.pms.domain.Application;

import java.io.IOException;
import java.util.List;

public interface ExcelService {

    byte[] exportApplicationsToExcel(List<Application> applications) throws IOException;
}
