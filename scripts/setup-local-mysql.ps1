<#
.SYNOPSIS
    D-PMS 로컬 MySQL 준비 (처음 한 번, 비밀번호를 바꾸고 싶을 때 -Rotate 로 다시 실행)

.DESCRIPTION
    1. 데이터베이스 2개 생성: dpms (개발 서버), dpms_test (자동 테스트 전용, 테스트가 매번 비우고 다시 만든다)
    2. 계정 2개를 역할별로 분리
         dpms_owner : 테이블 생성·변경 (Flyway 마이그레이션 전용)
         dpms_app   : 데이터 조회·등록·수정·삭제만 (서버 실행용, DDL 권한 없음)
    3. 비밀번호는 이 스크립트가 무작위로 만들어 config/dpms-secret.yml 에만 저장한다 (Git 제외 파일)

    MySQL 관리자(root) 비밀번호는 실행 중에 mysql 클라이언트가 직접 묻는다. 스크립트나 파일에 남지 않는다.

.EXAMPLE
    .\scripts\setup-local-mysql.ps1
    .\scripts\setup-local-mysql.ps1 -Rotate          # 두 계정 비밀번호를 새로 만든다
    .\scripts\setup-local-mysql.ps1 -MySqlExe "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
#>
param(
    [string]$MySqlExe = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$AdminUser = 'root',
    [switch]$Rotate
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$secretPath = Join-Path $projectRoot 'config\dpms-secret.yml'

if (-not (Test-Path $MySqlExe)) {
    throw "mysql.exe 를 찾을 수 없습니다: $MySqlExe  (-MySqlExe 로 경로를 지정하세요)"
}

# 영문 대소문자·숫자만 사용 (SQL·YAML 에서 따옴표 처리 문제가 없도록). 헷갈리는 0/O, 1/l/I 는 뺐다
function New-RandomPassword([int]$Length = 24) {
    $alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $buffer = New-Object byte[] 1
    $result = New-Object System.Text.StringBuilder
    try {
        while ($result.Length -lt $Length) {
            $rng.GetBytes($buffer)
            # 나머지 연산 편향을 없애기 위해 알파벳 길이의 배수 범위 안의 값만 쓴다
            if ($buffer[0] -lt (256 - (256 % $alphabet.Length))) {
                [void]$result.Append($alphabet[$buffer[0] % $alphabet.Length])
            }
        }
    } finally {
        $rng.Dispose()
    }
    return $result.ToString()
}

function Read-Secret([string]$Path, [string]$Key) {
    $line = Get-Content -Path $Path | Where-Object { $_ -match "^$Key\s*:" } | Select-Object -First 1
    if ($line) { return ($line -split ':', 2)[1].Trim() }
    return $null
}

$appPassword = $null
$ownerPassword = $null
if ((Test-Path $secretPath) -and -not $Rotate) {
    # 다시 실행해도 기존 비밀번호를 유지한다 (계정·권한만 다시 맞춤)
    $appPassword = Read-Secret $secretPath 'DPMS_DB_PASSWORD'
    $ownerPassword = Read-Secret $secretPath 'DPMS_DB_MIGRATION_PASSWORD'
}
if (-not $appPassword) { $appPassword = New-RandomPassword }
if (-not $ownerPassword) { $ownerPassword = New-RandomPassword }

# JDBC 는 127.0.0.1 로 접속한다. MySQL 이 이름 해석을 끈 환경(skip_name_resolve)에서도 맞도록 두 호스트 모두 만든다
$hosts = @('localhost', '127.0.0.1')
$sql = New-Object System.Text.StringBuilder
[void]$sql.AppendLine("CREATE DATABASE IF NOT EXISTS dpms CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
[void]$sql.AppendLine("CREATE DATABASE IF NOT EXISTS dpms_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
foreach ($h in $hosts) {
    [void]$sql.AppendLine("CREATE USER IF NOT EXISTS 'dpms_owner'@'$h' IDENTIFIED BY '$ownerPassword';")
    [void]$sql.AppendLine("ALTER USER 'dpms_owner'@'$h' IDENTIFIED BY '$ownerPassword';")
    [void]$sql.AppendLine("GRANT ALL PRIVILEGES ON dpms.* TO 'dpms_owner'@'$h';")
    [void]$sql.AppendLine("GRANT ALL PRIVILEGES ON dpms_test.* TO 'dpms_owner'@'$h';")

    [void]$sql.AppendLine("CREATE USER IF NOT EXISTS 'dpms_app'@'$h' IDENTIFIED BY '$appPassword';")
    [void]$sql.AppendLine("ALTER USER 'dpms_app'@'$h' IDENTIFIED BY '$appPassword';")
    [void]$sql.AppendLine("GRANT SELECT, INSERT, UPDATE, DELETE ON dpms.* TO 'dpms_app'@'$h';")
    [void]$sql.AppendLine("GRANT SELECT, INSERT, UPDATE, DELETE ON dpms_test.* TO 'dpms_app'@'$h';")
    # 동시성 테스트가 "두 번째 요청이 실제로 행 잠금을 기다리는지" 확인할 때 읽는 표 (잠금 대기 목록 조회만 허용)
    [void]$sql.AppendLine("GRANT SELECT ON performance_schema.data_lock_waits TO 'dpms_app'@'$h';")
}
[void]$sql.AppendLine("SELECT 'D-PMS MySQL setup done' AS result;")

Write-Host ''
Write-Host "MySQL($DbHost`:$Port) 에 '$AdminUser' 계정으로 접속합니다. 아래에 MySQL root 비밀번호를 입력하세요." -ForegroundColor Cyan
# SQL 은 표준 입력으로 넘긴다 (명령줄 인자로 넘기면 비밀번호가 프로세스 목록에 보일 수 있음)
$sql.ToString() | & $MySqlExe "--host=$DbHost" "--port=$Port" "--user=$AdminUser" --password --default-character-set=utf8mb4 --batch
if ($LASTEXITCODE -ne 0) {
    throw "MySQL 설정에 실패했습니다 (종료 코드 $LASTEXITCODE). root 비밀번호와 MySQL 서비스 실행 여부를 확인하세요."
}

$content = @"
# D-PMS 로컬 DB 접속 정보 - scripts/setup-local-mysql.ps1 이 생성 (Git 제외, 다른 사람과 공유 금지)
# 환경변수(DPMS_DB_*)가 있으면 환경변수가 우선한다
DPMS_DB_USERNAME: dpms_app
DPMS_DB_PASSWORD: $appPassword
DPMS_DB_MIGRATION_USERNAME: dpms_owner
DPMS_DB_MIGRATION_PASSWORD: $ownerPassword
"@
New-Item -ItemType Directory -Force -Path (Split-Path $secretPath) | Out-Null
# BOM 없는 UTF-8 (BOM 이 있으면 첫 번째 키 이름이 달라져 설정을 못 읽는다)
[System.IO.File]::WriteAllText($secretPath, $content, (New-Object System.Text.UTF8Encoding $false))

Write-Host ''
Write-Host "완료: 데이터베이스 dpms, dpms_test / 계정 dpms_owner, dpms_app" -ForegroundColor Green
Write-Host "접속 정보 파일: $secretPath (Git 에 올라가지 않음)" -ForegroundColor Green
