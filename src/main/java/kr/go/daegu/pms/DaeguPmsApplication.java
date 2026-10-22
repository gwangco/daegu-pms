package kr.go.daegu.pms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * D-PMS 기동 클래스.
 *
 * 업무 코드(kr.go.daegu.pms)와 함께 전자정부 공통컴포넌트(egovframework.com)의 서비스·DAO 도 Bean 으로 등록한다.
 * 공통컴포넌트는 eGov 배포본의 패키지 이름을 그대로 유지해야 원본과 비교·업데이트하기 쉽다.
 */
@SpringBootApplication(scanBasePackages = {"kr.go.daegu.pms", "egovframework.com"})
public class DaeguPmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DaeguPmsApplication.class, args);
	}

}
