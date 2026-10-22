package kr.go.daegu.pms;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 테스트 DB 초기화.
 *
 * 테스트 컨텍스트가 뜰 때 dpms_test 를 비우고(clean) V1 부터 다시 적용(migrate)한다.
 * H2 인메모리 DB 를 쓸 때처럼 매번 같은 데이터에서 시작하게 하려는 것이다.
 * 설정 실수로 개발·운영 DB 에 연결된 경우 데이터를 지우지 않도록, DB 이름이 _test 로 끝나지 않으면 중단한다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestDatabaseConfig {

    @Bean
    FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            String database = currentDatabase(flyway);
            if (database == null || !database.endsWith("_test")) {
                throw new IllegalStateException("테스트 전용 DB(이름이 _test 로 끝나는 DB)가 아니라서 초기화를 중단했습니다: " + database);
            }
            flyway.clean();
            flyway.migrate();
        };
    }

    private static String currentDatabase(Flyway flyway) {
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
            return connection.getCatalog();
        } catch (SQLException e) {
            throw new IllegalStateException("테스트 DB 이름을 확인하지 못했습니다.", e);
        }
    }
}
