"""
DB 산출물(테이블 정의서, ERD)을 실제 DB 의 information_schema 에서 만든다.

사용: python scripts/generate-db-docs.py
  - 접속 계정: 환경변수 DPMS_DB_USERNAME / DPMS_DB_PASSWORD, 없으면 config/dpms-secret.yml
  - DB 이름: 환경변수 DPMS_DB_NAME (기본 dpms), mysql 클라이언트: 환경변수 MYSQL_BIN (기본 PATH 의 mysql)
  - 결과: docs/si/TABLE_DEFINITION.md, docs/si/ERD.md (덮어씀)

설명은 DDL 의 COMMENT, 관계는 외래키에서 가져온다. 외래키 없이 값으로 잇는 관계만 아래 LOGICAL_RELATIONS 에 적는다.
마이그레이션을 추가하면 로컬 DB 에 적용(서버 기동)한 뒤 다시 실행한다.
"""
import datetime
import json
import os
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "docs" / "si"

BUSINESS = ["users", "login_history", "privacy_access_log", "announcement", "application",
            "evaluation", "settlement", "settlement_history"]
COMMON = ["comtccmmnclcode", "comtccmmncode", "comtccmmndetailcode", "comtecopseq", "comtnbbsmaster",
          "comtnbbs", "comtnfaqinfo", "comtnfile", "comtnfiledetail"]
BATCH = {
    "batch_job_instance": "잡 이름 + 식별 파라미터 = 실행 단위. 같은 기준일은 한 번만 성공",
    "batch_job_execution": "실행 1회의 시작·종료 시각, 상태, 종료 코드",
    "batch_job_execution_params": "실행 파라미터 (targetDate, requestedAt, requestedBy)",
    "batch_job_execution_context": "잡 재시작에 쓰는 상태",
    "batch_step_execution": "스텝 실행의 읽기·쓰기·커밋 건수",
    "batch_step_execution_context": "스텝 재시작에 쓰는 상태 (리더 위치)",
    "batch_job_seq": "잡 인스턴스 번호 채번",
    "batch_job_execution_seq": "잡 실행 번호 채번",
    "batch_step_execution_seq": "스텝 실행 번호 채번",
}
MANAGEMENT = {"flyway_schema_history": "Flyway 마이그레이션 적용 이력 (버전, 체크섬, 적용 일시)"}

FK_LABELS = {
    "fk_announcement_writer": "작성", "fk_application_announcement": "접수", "fk_application_company": "신청",
    "fk_application_plan_atch_file": "사업계획서", "fk_evaluation_application": "평가",
    "fk_evaluation_evaluator": "채점", "fk_settlement_application": "집행", "fk_settlement_receipt_atch_file": "증빙",
    "fk_settlement_reviewed_by": "검수", "fk_settlement_history_settlement": "처리 이력",
    "COMTCCMMNCODE_FK1": "분류", "COMTCCMMNDETAILCODE_FK1": "상세 코드", "COMTNBBS_FK1": "게시물",
    "COMTNFAQINFO_FK1": "첨부", "COMTNFILEDETAIL_FK1": "파일",
    "JOB_INST_EXEC_FK": "실행", "JOB_EXEC_CTX_FK": "상태", "JOB_EXEC_PARAMS_FK": "파라미터",
    "JOB_EXEC_STEP_FK": "스텝", "STEP_EXEC_CTX_FK": "상태",
}

# (부모, 자식, 자식 컬럼, 부모 쪽 필수 여부, 이름, 설명) — 외래키 없이 값으로 잇는 관계
LOGICAL_RELATIONS = [
    ("users", "login_history", "user_id", False, "접속", "접속기록은 계정과 묶지 않아 계정을 정리해도 남는다. 없는 아이디로 실패하면 비운다"),
    ("users", "privacy_access_log", "user_id", True, "처리자", "처리 기록은 계정과 묶지 않아 계정을 정리해도 남는다"),
    ("users", "privacy_access_log", "subject_id", False, "정보주체", "같은 이유. 여러 기업이 담긴 엑셀·목록은 비운다"),
    ("users", "settlement_history", "actor_id", True, "처리자", "처리 이력은 계정과 묶지 않아 계정을 정리해도 남는다"),
    ("users", "comtnbbs", "NTCR_ID", True, "게시자", "공통컴포넌트 원본 DDL 에 외래키가 없다"),
    ("comtccmmndetailcode", "announcement", "field_code", True, "분야 FIELD",
     "공통코드 키가 (코드ID, 코드) 두 컬럼이라 외래키 대신 서비스가 공통코드 목록으로 검증한다"),
    ("comtccmmndetailcode", "settlement", "item_code", True, "비목 EXP_ITEM",
     "공통코드 키가 (코드ID, 코드) 두 컬럼이라 외래키 대신 서비스가 공통코드 목록으로 검증한다"),
    ("comtnfile", "comtnbbs", "ATCH_FILE_ID", False, "공지 첨부", "공통컴포넌트 원본 DDL 에 외래키가 없다"),
]


def display(name):
    return name.upper() if name.startswith(("comt", "batch_")) else name


def unescape(value):
    # mysql -B 출력의 이스케이프(\n, \t, \\)를 푼다
    return re.sub(r"\\(.)", lambda m: {"n": " ", "t": " ", "0": ""}.get(m.group(1), m.group(1)), value)


def clean_sql(expr):
    return expr.replace("\\'", "'").replace("_utf8mb4", "").replace("`", "")


def md(text):
    return (text or "").replace("|", "\\|")


def credentials():
    user, password = os.environ.get("DPMS_DB_USERNAME"), os.environ.get("DPMS_DB_PASSWORD")
    secret_file = ROOT / "config" / "dpms-secret.yml"
    if (not user or not password) and secret_file.exists():
        secret = {}
        for line in secret_file.read_text(encoding="utf-8").splitlines():
            if ":" in line and not line.lstrip().startswith("#"):
                key, value = line.split(":", 1)
                secret[key.strip()] = value.strip().strip("\"'")
        user = user or secret.get("DPMS_DB_USERNAME")
        password = password or secret.get("DPMS_DB_PASSWORD")
    if not user or not password:
        raise SystemExit("DB 접속 정보가 없습니다: DPMS_DB_USERNAME/DPMS_DB_PASSWORD 또는 config/dpms-secret.yml")
    return user, password


def mysql_bin():
    candidates = [os.environ.get("MYSQL_BIN"), shutil.which("mysql"),
                  r"C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"]
    for candidate in candidates:
        if candidate and Path(candidate).exists():
            return candidate
    raise SystemExit("mysql 클라이언트를 찾지 못했습니다: 환경변수 MYSQL_BIN 으로 지정하세요")


class Schema:
    def __init__(self):
        self.db = os.environ.get("DPMS_DB_NAME", "dpms")
        user, password = credentials()
        self.cmd = [mysql_bin(), "-u", user, "-h", os.environ.get("DPMS_DB_HOST", "127.0.0.1"),
                    "--default-character-set=utf8mb4", "-N", "-B"]
        self.env = dict(os.environ, MYSQL_PWD=password)   # 비밀번호를 명령줄에 남기지 않는다

    def query(self, sql):
        res = subprocess.run(self.cmd + ["-e", sql, self.db], env=self.env, capture_output=True, check=True)
        return [[None if v == "NULL" else unescape(v) for v in line.split("\t")]
                for line in res.stdout.decode("utf-8").splitlines()]

    def load(self):
        where = f"TABLE_SCHEMA = '{self.db}'"
        self.tables = {t: c for t, c in self.query(
            f"SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES WHERE {where} AND TABLE_TYPE = 'BASE TABLE'")}
        self.columns = {}
        for row in self.query(
                "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, EXTRA, "
                f"COLUMN_COMMENT, GENERATION_EXPRESSION FROM information_schema.COLUMNS WHERE {where} "
                "ORDER BY TABLE_NAME, ORDINAL_POSITION"):
            keys = ["table", "name", "type", "nullable", "default", "key", "extra", "comment", "generation"]
            self.columns.setdefault(row[0], []).append(dict(zip(keys, row)))
        self.fks = [dict(zip(["table", "column", "ref_table", "ref_column", "name"], row)) for row in self.query(
            "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME, CONSTRAINT_NAME "
            f"FROM information_schema.KEY_COLUMN_USAGE WHERE {where} AND REFERENCED_TABLE_NAME IS NOT NULL "
            "ORDER BY TABLE_NAME, CONSTRAINT_NAME")]
        self.indexes = [dict(zip(["table", "name", "unique", "columns"], [t, n, u == "0", c])) for t, n, u, c in self.query(
            "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') "
            f"FROM information_schema.STATISTICS WHERE {where} GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE "
            "ORDER BY TABLE_NAME, INDEX_NAME")]
        self.checks = [dict(zip(["table", "name", "clause"], row)) for row in self.query(
            "SELECT tc.TABLE_NAME, cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS cc "
            "JOIN information_schema.TABLE_CONSTRAINTS tc ON tc.CONSTRAINT_SCHEMA = cc.CONSTRAINT_SCHEMA "
            f"AND tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME WHERE cc.CONSTRAINT_SCHEMA = '{self.db}' "
            "ORDER BY tc.TABLE_NAME, cc.CONSTRAINT_NAME")]
        self.version = self.query("SELECT version FROM flyway_schema_history WHERE success = 1 "
                                  "ORDER BY installed_rank DESC LIMIT 1")[0][0]
        return self


def key_marks(schema, table, col):
    marks = []
    if col["key"] == "PRI":
        marks.append("PK")
    if any(i["unique"] and i["name"] != "PRIMARY" and col["name"] in i["columns"].split(", ")
           for i in schema.indexes if i["table"] == table):
        marks.append("UK")
    if any(f["table"] == table and f["column"] == col["name"] for f in schema.fks):
        marks.append("FK")
    if "auto_increment" in col["extra"]:
        marks.append("AI")
    if "GENERATED" in col["extra"] and col["generation"]:
        marks.append("생성")
    return ", ".join(marks)


def default_text(col):
    value = col["default"]
    text = "" if value is None else clean_sql(value)
    if "on update" in col["extra"].lower():
        text += " (수정 시 갱신)"
    return text


def table_section(schema, table, number):
    lines = [f"### {number} {display(table)} · {md(schema.tables[table]) or '-'}", "",
             "| # | 컬럼 | 타입 | NULL | 기본값 | 키 | 설명 |",
             "| ---: | :--- | :--- | :---: | :--- | :--- | :--- |"]
    for i, col in enumerate(schema.columns[table], 1):
        lines.append(f"| {i} | `{col['name']}` | {col['type'].upper()} | {'Y' if col['nullable'] == 'YES' else 'N'} "
                     f"| {md(default_text(col))} | {key_marks(schema, table, col)} | {md(col['comment'])} |")
    notes = []
    indexes = [i for i in schema.indexes if i["table"] == table and i["name"] != "PRIMARY"
               and not any(f["name"] == i["name"] for f in schema.fks)]
    if indexes:
        notes.append("인덱스: " + ", ".join(f"`{i['name']}` ({i['columns']}{', 유일' if i['unique'] else ''})" for i in indexes))
    fks = [f for f in schema.fks if f["table"] == table]
    if fks:
        notes.append("외래키: " + ", ".join(f"`{f['column']}` → {display(f['ref_table'])}.`{f['ref_column']}`" for f in fks))
    for c in [c for c in schema.checks if c["table"] == table]:
        notes.append(f"CHECK `{c['name']}`: `{clean_sql(c['clause'])}`")
    for col in schema.columns[table]:
        if col["generation"]:
            notes.append(f"생성 컬럼 `{col['name']}` = `{clean_sql(col['generation'])}`")
    lines += [""] + [f"- {n}" for n in notes] if notes else []
    return "\n".join(lines) + "\n"


def table_definition(schema, today):
    others = sorted(set(schema.tables) - set(BUSINESS) - set(COMMON) - set(BATCH) - set(MANAGEMENT))
    out = ["# 테이블 정의서", "",
           "> 이 문서는 `scripts/generate-db-docs.py` 가 MySQL 의 `information_schema` 에서 만든다. "
           "손으로 고치지 말고, 마이그레이션을 적용한 뒤 스크립트를 다시 실행한다.", "",
           f"- 기준: Flyway V{schema.version} 적용 DB, 생성일 {today}",
           f"- 테이블: 업무 {len(BUSINESS)} · 공통컴포넌트 {len(COMMON)} · 배치 메타데이터 {len(BATCH)} · 관리 {len(MANAGEMENT)}"
           + (f" · 분류 전 {len(others)}" if others else ""),
           "- 키: PK 기본키, UK 유일, FK 외래키, AI 자동 증가, 생성 = 다른 컬럼으로 계산하는 생성 컬럼. NULL 이 Y 면 비워 둘 수 있다",
           "- 관계 그림은 [ERD](ERD.md) 에 있다", "",
           "## 목차", "", "| 구분 | 테이블 | 설명 | 컬럼 수 |", "| :--- | :--- | :--- | ---: |"]
    for group, names in [("업무", BUSINESS), ("공통컴포넌트", COMMON)]:
        for name in names:
            out.append(f"| {group} | {display(name)} | {md(schema.tables.get(name))} | {len(schema.columns.get(name, []))} |")
    out.append("")
    sections = [("1. 업무 테이블", BUSINESS), ("2. 공통컴포넌트 테이블", COMMON)]
    if others:
        sections.append(("분류 전 테이블 (스크립트의 목록에 추가할 것)", others))
    for title, names in sections:
        out += [f"## {title}", ""]
        prefix = title.split(".")[0]
        for i, name in enumerate(names, 1):
            out.append(table_section(schema, name, f"{prefix}.{i}"))
    out += ["## 3. 배치 메타데이터 (Spring Batch 5.2)", "",
            "Flyway V7 이 spring-batch-core 의 `schema-mysql.sql` 을 그대로 만든다. 컬럼 정의는 그 스크립트와 같다.", "",
            "| 테이블 | 용도 | 컬럼 수 |", "| :--- | :--- | ---: |"]
    out += [f"| {display(t)} | {d} | {len(schema.columns.get(t, []))} |" for t, d in BATCH.items()]
    out += ["", "## 4. 관리 테이블", "", "| 테이블 | 용도 | 컬럼 수 |", "| :--- | :--- | ---: |"]
    out += [f"| {t} | {d} | {len(schema.columns.get(t, []))} |" for t, d in MANAGEMENT.items()]
    return "\n".join(out) + "\n"


def short_comment(comment):
    text = (comment or "").split(" (")[0].replace('"', "'")
    if len(text) > 30 and ", " in text[:30]:
        text = text[:text.rfind(", ", 0, 30)] + " 등"
    return text[:30]


def mermaid_entities(schema, names, detail):
    logical = {(child, column) for _, child, column, _, _, _ in LOGICAL_RELATIONS}
    lines = []
    for table in names:
        attrs = []
        for col in schema.columns[table]:
            marks = [m for m in key_marks(schema, table, col).split(", ") if m in ("PK", "UK", "FK")]
            core = col["name"] in ("status", "role", "receipt_no", "total_amount", "total_score")
            if marks or (detail and (core or (table, col["name"]) in logical)):
                base = col["type"].split("(")[0].split(" ")[0]
                comment = short_comment(col["comment"])
                attrs.append(f'        {base} {col["name"]}{" " + ", ".join(marks) if marks else ""}'
                             + (f' "{comment}"' if comment else ""))
        lines += [f"    {display(table)} {{"] + attrs + ["    }"]
    return lines


def mermaid_relations(schema, names):
    lines = []
    for fk in schema.fks:
        if fk["table"] in names and fk["ref_table"] in names:
            col = next(c for c in schema.columns[fk["table"]] if c["name"] == fk["column"])
            parent = "||" if col["nullable"] == "NO" else "|o"
            lines.append(f'    {display(fk["ref_table"])} {parent}--o{{ {display(fk["table"])} : "{FK_LABELS.get(fk["name"], fk["column"])}"')
    return lines


def erd(schema, today):
    names = BUSINESS + COMMON
    out = ["# ERD", "",
           "> 이 문서는 `scripts/generate-db-docs.py` 가 실제 DB 의 외래키와 컬럼 설명으로 만든다. "
           "실선은 외래키, 점선은 외래키 없이 값으로 잇는 관계다.", "",
           f"- 기준: Flyway V{schema.version} 적용 DB, 생성일 {today}",
           "- 칸에는 키 컬럼과 상태 같은 핵심 컬럼만 그렸다. 전체 컬럼은 [테이블 정의서](TABLE_DEFINITION.md) 에 있다", "",
           "## 1. 업무와 공통컴포넌트", "", "```mermaid", "erDiagram"]
    out += mermaid_relations(schema, names)
    for parent, child, column, required, label, _ in LOGICAL_RELATIONS:
        out.append(f'    {display(parent)} {"||" if required else "|o"}..o{{ {display(child)} : "{label}"')
    out += mermaid_entities(schema, names, True) + ["```", "",
           "## 2. 배치 메타데이터", "", "```mermaid", "erDiagram"]
    out += mermaid_relations(schema, list(BATCH)) + mermaid_entities(schema, [t for t in BATCH if not t.endswith("_seq")], False)
    out += ["```", "", "## 3. 외래키 없이 잇는 관계", "",
            "| 관계 | 컬럼 | 외래키를 두지 않은 이유 |", "| :--- | :--- | :--- |"]
    out += [f"| {display(p)} → {display(c)} ({label}) | `{display(c)}.{col}` | {why} |"
            for p, c, col, _, label, why in LOGICAL_RELATIONS]
    return "\n".join(out) + "\n"


def main():
    schema = Schema().load()
    today = datetime.date.today().isoformat()
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    (OUT_DIR / "TABLE_DEFINITION.md").write_text(table_definition(schema, today), encoding="utf-8", newline="\n")
    (OUT_DIR / "ERD.md").write_text(erd(schema, today), encoding="utf-8", newline="\n")
    print(json.dumps({"version": schema.version, "tables": len(schema.tables), "foreignKeys": len(schema.fks)}))


if __name__ == "__main__":
    main()
