package kr.go.daegu.pms;

import org.junit.jupiter.api.Test;

/**
 * 스프링 컨텍스트 기동 확인.
 * 다른 통합 테스트와 같은 설정(IntegrationTestSupport)을 사용해야 컨텍스트가 1개로 재사용된다.
 * (설정이 다르면 컨텍스트가 하나 더 뜨면서 테스트 DB 를 다시 초기화하므로 전체 실행 시간이 늘어난다)
 */
class DaeguPmsApplicationTests extends IntegrationTestSupport {

	@Test
	void contextLoads() {
	}

}
