// eGovFrame VSCode Initializr 5.0.7 의 "CRUD 코드 생성" 과 같은 방식으로 템플릿을 렌더링한다.
// - 템플릿: 확장 설치 폴더의 templates/code/*.hbs (원본 그대로 사용)
// - 컨텍스트/헬퍼/타입매핑/출력 경로: 확장 dist/extension.js 의 fi(), tr(), Lf(), X1(), registerHelper 와 동일하게 구성
// 사용: node egov-codegen.mjs <ddl파일> <패키지명> <출력폴더>
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const Handlebars = require('handlebars');

// eGov VS Code 확장 설치 경로 (환경변수 EGOV_EXT_DIR 로 지정, 예: ~/.vscode/extensions/egovframework.egovframe-vscode-initializr-5.0.7-universal)
const EXT = process.env.EGOV_EXT_DIR;
if (!EXT) { console.error("EGOV_EXT_DIR 환경변수에 eGov VS Code 확장 설치 경로를 지정하세요."); process.exit(1); }
const [ddlFile, packageName, outDir] = process.argv.slice(2);
const ddl = fs.readFileSync(ddlFile, 'utf8');

// --- 확장과 동일한 이름 규칙: S_ (snake → camel), $_ (첫 글자 대문자) ---
const camel = s => s.toLowerCase().replace(/_([a-z0-9])/g, (_, c) => c.toUpperCase());
const pascal = s => s && s.charAt(0).toUpperCase() + s.slice(1);
// --- 확장과 동일한 DB 타입 → Java 타입 매핑 (Lf) ---
const TYPE = { VARCHAR: 'java.lang.String', VARCHAR2: 'java.lang.String', CHAR: 'java.lang.String', TEXT: 'java.lang.String',
  MEDIUMTEXT: 'java.lang.String', INT: 'java.lang.Integer', INTEGER: 'java.lang.Integer', NUMBER: 'java.lang.Integer',
  BIGINT: 'java.lang.Long', DECIMAL: 'java.math.BigDecimal', DATE: 'java.sql.Date', DATETIME: 'java.util.Date',
  TIMESTAMP: 'java.sql.Timestamp' };

// --- DDL 파싱 (tr 와 같은 규칙: 첫 단어=컬럼명, 둘째 단어=타입, PRIMARY KEY 절 인식) ---
const flat = ddl.replace(/\s+/g, ' ').trim();
const dbTableName = /CREATE\s+TABLE\s+`?(\w+)`?/i.exec(flat)[1];
const tableName = pascal(camel(dbTableName));
const body = /\((.*)\)/s.exec(ddl)[1];
const pkCols = (/PRIMARY KEY\s*\(([^)]+)\)/i.exec(flat)?.[1] ?? '').split(',').map(s => s.trim()).filter(Boolean);
const attributes = [];
for (const raw of body.split(/,(?![^(]*\))/).map(s => s.replace(/\s+/g, ' ').trim()).filter(Boolean)) {
  if (/^(PRIMARY KEY|UNIQUE KEY|KEY|CONSTRAINT|FOREIGN KEY)/i.test(raw)) continue;
  const [col, typeRaw] = raw.split(' ');
  const dataType = /^\w+/.exec(typeRaw.toUpperCase())[0];
  const ccName = camel(col);
  attributes.push({ ccName, columnName: col, isPrimaryKey: pkCols.includes(col) || /PRIMARY KEY/i.test(raw),
    pcName: pascal(ccName), dataType, javaType: TYPE[dataType] || 'java.lang.Object' });
}
const pkAttributes = attributes.filter(a => a.isPrimaryKey);

// --- 확장과 동일한 템플릿 컨텍스트 (fi + packagePath) ---
const ctx = { tableName, dbTableName, attributes, pkAttributes, packageName, className: tableName,
  classNameFirstCharLower: tableName[0].toLowerCase() + tableName.slice(1), author: 'author',
  date: new Date().toISOString().split('T')[0], version: '1.0.0', packagePath: packageName.replace(/\./g, '/') };

// --- 확장과 동일한 헬퍼 ---
Handlebars.registerHelper('eq', (a, b) => a === b);
Handlebars.registerHelper('concat', (...a) => a.slice(0, -1).join(''));
Handlebars.registerHelper('setVar', (name, value, options) => { options.data.root[name] = value; });
Handlebars.registerHelper('lowercase', s => typeof s !== 'string' ? '' : s.toLowerCase());
Handlebars.registerHelper('add', (a, b) => (Number(a) || 0) + (Number(b) || 0));

// --- 확장과 동일한 출력 경로 (X1). 이 프로젝트는 Thymeleaf 를 쓰므로 JSP 템플릿은 제외 ---
const t = ctx.tableName, i = t[0].toLowerCase() + t.slice(1), n = ctx.packagePath;
const outputs = [
  ['sample-vo-template.hbs', `src/main/java/${n}/service/${t}VO.java`],
  ['sample-default-vo-template.hbs', `src/main/java/${n}/service/${t}DefaultVO.java`],
  ['sample-controller-template.hbs', `src/main/java/${n}/web/${t}Controller.java`],
  ['sample-service-template.hbs', `src/main/java/${n}/service/${t}Service.java`],
  ['sample-service-impl-template.hbs', `src/main/java/${n}/service/impl/${t}ServiceImpl.java`],
  ['sample-mapper-interface-template.hbs', `src/main/java/${n}/service/impl/${t}Mapper.java`],
  ['sample-mapper-template.hbs', `src/main/resources/mapper/${t}_SQL.xml`],
  ['sample-thymeleaf-list.hbs', `src/main/resources/templates/thymeleaf/${i}/${i}List.html`],
  ['sample-thymeleaf-register.hbs', `src/main/resources/templates/thymeleaf/${i}/${i}Register.html`],
];
for (const [tpl, out] of outputs) {
  const src = fs.readFileSync(path.join(EXT, 'templates', 'code', tpl), 'utf8');
  const dest = path.join(outDir, out);
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  fs.writeFileSync(dest, Handlebars.compile(src)(ctx));
  console.log('generated', out);
}
console.log('context:', JSON.stringify({ tableName, dbTableName, pk: pkAttributes.map(a => a.columnName),
  attributes: attributes.map(a => `${a.columnName}:${a.dataType}->${a.ccName}(${a.javaType})`) }, null, 1));
