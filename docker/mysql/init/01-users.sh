#!/bin/bash
# DB 계정을 둘로 나눈다 (로컬 scripts/setup-local-mysql.ps1, CI 와 같은 구성)
#   dpms_owner : Flyway 가 테이블을 만들고 바꾸는 계정 (dpms 의 모든 권한)
#   dpms_app   : 서버 실행 계정 (조회·등록·수정·삭제만)
# MySQL 컨테이너는 데이터 볼륨이 비어 있을 때 한 번만 이 파일을 실행한다.
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE USER 'dpms_owner'@'%' IDENTIFIED BY '${DPMS_DB_MIGRATION_PASSWORD}';
GRANT ALL PRIVILEGES ON dpms.* TO 'dpms_owner'@'%';
CREATE USER 'dpms_app'@'%' IDENTIFIED BY '${DPMS_DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON dpms.* TO 'dpms_app'@'%';
SQL
