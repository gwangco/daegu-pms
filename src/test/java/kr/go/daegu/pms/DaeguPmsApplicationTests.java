package kr.go.daegu.pms;

import org.junit.jupiter.api.Test;

/**
 * 스프링 컨텍스트 기동 확인.
 * 다른 통합 테스트와 같은 설정(IntegrationTestSupport)을 사용해야 컨텍스트가 1개로 재사용된다.
 * (설정이 다르면 컨텍스트가 하나 더 뜨면서 같은 인메모리 DB 에 data.sql 을 다시 넣다가 PK 중복 오류가 난다)
 */
class DaeguPmsApplicationTests extends IntegrationTestSupport {

	@Test
	void contextLoads() {
	}

}
