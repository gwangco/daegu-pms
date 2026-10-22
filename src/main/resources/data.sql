-- D-PMS Initial Seed Data
-- ※ AUTO_INCREMENT PK는 직접 지정하지 않는다.
--   PK를 직접 넣으면 H2/MariaDB의 자동증가 시퀀스가 따라오지 않아
--   화면에서 신규 등록 시 "PK 중복" 오류가 발생할 수 있기 때문이다.
--   대신 INSERT 순서대로 1, 2, 3 ... 이 부여되는 것을 전제로 FK를 참조한다.

-- 1. 공통코드 그룹
INSERT INTO com_code_group (group_id, group_name, description, use_yn) VALUES
('FIELD', '지원 분야', '대구 신산업 R&D 및 기술지원 5대 전략 분야', 'Y'),
('APP_STATUS', '신청서 진행상태', '지원사업 신청부터 협약까지의 상태머신', 'Y'),
('EXP_ITEM', '사업비 비목분류', '보조금 관리규정에 따른 사업비 집행 비목', 'Y'),
('SETTLE_STATUS', '정산 검수상태', '사업비 영수증 검수 상태', 'Y');

-- 1-1. 채번 테이블 초기값: seed 신청서가 접수번호 1~3 을 사용하므로 다음 발급 번호는 4
INSERT INTO ids (table_name, next_id) VALUES ('APPLICATION_RECEIPT', 4);
-- 공지 ID: seed 공지 1~3 사용, 다음 발급 번호 4
INSERT INTO ids (table_name, next_id) VALUES ('NOTICE', 4);

-- 2. 공통코드 상세
INSERT INTO com_code_detail (group_id, code_id, code_name, sort_order, use_yn) VALUES
('FIELD', 'MOBILITY', '미래 모빌리티/부품', 1, 'Y'),
('FIELD', 'ROBOT', '첨단 로봇/자동화', 2, 'Y'),
('FIELD', 'AI_SW', '인공지능(AI)·빅데이터', 3, 'Y'),
('FIELD', 'HEALTH', '디지털 헬스케어', 4, 'Y'),
('FIELD', 'SMART_FAB', '스마트 제조/공구금형', 5, 'Y'),

('APP_STATUS', 'TEMP_SAVED', '임시저장', 1, 'Y'),
('APP_STATUS', 'SUBMITTED', '접수완료', 2, 'Y'),
('APP_STATUS', 'DOCUMENT_PASSED', '서류적격통과', 3, 'Y'),
('APP_STATUS', 'EVALUATING', '전문가심사중', 4, 'Y'),
('APP_STATUS', 'SELECTED', '최종선정(협약)', 5, 'Y'),
('APP_STATUS', 'REJECTED', '심사탈락', 6, 'Y'),

('EXP_ITEM', 'EXP01', '인건비 (연구원/개발자)', 1, 'Y'),
('EXP_ITEM', 'EXP02', '직접재료비 및 시약구입비', 2, 'Y'),
('EXP_ITEM', 'EXP03', '시제품 제작 및 가공비', 3, 'Y'),
('EXP_ITEM', 'EXP04', '연구활동비 (출장/여비)', 4, 'Y'),
('EXP_ITEM', 'EXP05', '위탁정산 및 수수료', 5, 'Y'),

('SETTLE_STATUS', 'REQUESTED', '검수요청', 1, 'Y'),
('SETTLE_STATUS', 'APPROVED', '검수승인', 2, 'Y'),
('SETTLE_STATUS', 'REJECTED', '보완(반려)', 3, 'Y');

-- 3. 테스트 계정
--    비밀번호는 BCrypt 단방향 해시로 저장 (원문: admin1234 / manager1234 / eval1234 / company1234)
--    이메일·전화번호는 실제 기관·개인에게 닿지 않도록 예약 도메인(example.com)과 가상 번호만 쓴다
--    {bcrypt} 접두어는 DelegatingPasswordEncoder가 어떤 알고리즘으로 검증할지 판단하는 표식
INSERT INTO users (user_id, password, user_name, email, phone, company_name, business_no, role, enabled) VALUES
('admin', '{bcrypt}$2a$10$Sb920P3ksKEDtwLIDYUloud/EMTbsGI8Dby4TFSk1k1K03A3Y4M0C', '총괄관리자', 'admin@example.com', '010-0000-1001', '대구광역시청 미래혁신성장실', '504-83-00001', 'ROLE_ADMIN', TRUE),
('manager1', '{bcrypt}$2a$10$E6.EeqgkNrLM8NfUpmPgwOVb1VQEA4FEzrmhaLuq08fN0L7bRoRMG', '김담당', 'manager1@example.com', '010-0000-2001', '대구디지털혁신진흥원(DIP)', '504-82-12345', 'ROLE_MANAGER', TRUE),
('evaluator1', '{bcrypt}$2a$10$GXAEa62sLir/mJ0UESxhZudL1OHDmNhEefybuuvFOFDmHzJWYErhi', '박심사', 'evaluator1@example.com', '010-0000-3001', '지역대학 IT융합학과(평가위원)', '502-83-99999', 'ROLE_EVALUATOR', TRUE),
('evaluator2', '{bcrypt}$2a$10$GXAEa62sLir/mJ0UESxhZudL1OHDmNhEefybuuvFOFDmHzJWYErhi', '정평가', 'evaluator2@example.com', '010-0000-3002', '지역대학 기계공학과(평가위원)', '502-82-88888', 'ROLE_EVALUATOR', TRUE),
('company1', '{bcrypt}$2a$10$UmRKKM8Acu97mqwuicfeTOLiFYLPrTFCyY2dob86qY7IzzPvANZk.', '이대표', 'ceo@mobility.example.com', '010-0000-4001', '(주)대구모빌리티솔루션', '504-81-98765', 'ROLE_COMPANY', TRUE),
('company2', '{bcrypt}$2a$10$UmRKKM8Acu97mqwuicfeTOLiFYLPrTFCyY2dob86qY7IzzPvANZk.', '최기술', 'cto@robotics.example.com', '010-0000-4002', '(주)수성AI로보틱스', '504-86-54321', 'ROLE_COMPANY', TRUE);

-- 4. 지원사업 공고 샘플
--    (1~11) 지난 공고 이력 (페이징 확인용) / (12~16) 현재 공고
INSERT INTO announcement (title, field_code, total_budget, max_grant, start_date, end_date, content, status, view_count, writer_id, created_at) VALUES
('2025년 대구 자율주행 실증 부품 시험인증 지원사업', 'MOBILITY', 700000000, 50000000, '2025-03-02', '2025-03-31', '자율주행 부품 기업의 KC·ISO 26262 시험인증 비용을 지원합니다.', 'CLOSED', 410, 'manager1', '2025-02-20 09:00:00'),
('2025년 대구 의료기기 임상 컨설팅 바우처', 'HEALTH', 300000000, 30000000, '2025-03-10', '2025-04-10', '디지털 헬스케어 기업의 식약처 인허가 및 임상 설계 컨설팅을 지원합니다.', 'CLOSED', 215, 'manager1', '2025-03-01 09:00:00'),
('2025년 스마트공장 고도화 공정데이터 구축 지원', 'SMART_FAB', 900000000, 80000000, '2025-04-01', '2025-04-30', '성서·달성산단 제조기업의 MES·공정데이터 수집 체계 구축을 지원합니다.', 'CLOSED', 377, 'manager1', '2025-03-20 09:00:00'),
('2025년 AI 바우처 수요기업 매칭 지원사업', 'AI_SW', 500000000, 30000000, '2025-05-01', '2025-05-31', '지역 중소기업과 AI 솔루션 공급기업 간 매칭 바우처를 지원합니다.', 'CLOSED', 520, 'manager1', '2025-04-21 09:00:00'),
('2025년 협동로봇 현장 실증 지원사업', 'ROBOT', 600000000, 60000000, '2025-05-15', '2025-06-15', '뿌리산업 현장에 협동로봇을 도입하는 실증 과제를 지원합니다.', 'CLOSED', 298, 'manager1', '2025-05-06 09:00:00'),
('2025년 전기차 구동부품 경량화 기술개발', 'MOBILITY', 1200000000, 100000000, '2025-06-02', '2025-07-02', '전기차 감속기·모터 하우징 경량화 소재 부품 기술개발을 지원합니다.', 'CLOSED', 466, 'manager1', '2025-05-25 09:00:00'),
('2025년 디지털치료기기 실증 테스트베드 이용 지원', 'HEALTH', 250000000, 25000000, '2025-07-01', '2025-07-31', '디지털치료기기 개발 기업의 병원 테스트베드 이용료를 지원합니다.', 'CLOSED', 187, 'manager1', '2025-06-20 09:00:00'),
('2025년 공구금형 디지털 전환(DX) 지원사업', 'SMART_FAB', 400000000, 40000000, '2025-08-01', '2025-08-29', '북성로 공구골목 및 금형기업의 3D 설계·가공 데이터 전환을 지원합니다.', 'CLOSED', 233, 'manager1', '2025-07-22 09:00:00'),
('2025년 하반기 AI 데이터 라벨링 지원사업', 'AI_SW', 350000000, 20000000, '2025-09-01', '2025-09-30', 'AI 학습용 데이터 구축 및 라벨링 비용을 지원합니다.', 'CLOSED', 341, 'manager1', '2025-08-25 09:00:00'),
('2026년 물류 자율이동로봇(AMR) 시제품 지원', 'ROBOT', 550000000, 50000000, '2026-03-02', '2026-04-03', '물류센터용 AMR 시제품 제작 및 성능평가 비용을 지원합니다.', 'CLOSED', 274, 'manager1', '2026-02-23 09:00:00'),
('2026년 상반기 미래차 전장부품 SW 검증 지원', 'MOBILITY', 650000000, 50000000, '2026-04-06', '2026-05-08', '차량용 SW 기능안전 및 사이버보안 검증 비용을 지원합니다.', 'CLOSED', 305, 'manager1', '2026-03-30 09:00:00'),

('대구 로봇산업 혁신클러스터 청년 스타트업 스케일업 지원사업', 'ROBOT', 600000000, 70000000, '2026-09-01', '2026-10-15',
'협동로봇 및 물류 자동화 로봇 시제품 제작을 위한 재료비 및 전문 가공비를 지원합니다.',
'CLOSED', 312, 'manager1', '2026-08-25 09:00:00'),

('2026년 대구 미래 모빌리티 부품 R&D 및 상용화 지원사업 공고', 'MOBILITY', 1500000000, 100000000, '2026-10-01', '2026-11-30',
'대구광역시 내 미래 모빌리티 관련 전장부품, 구동모터, 자율주행 센서 개발 기업을 대상으로 시제품 제작 및 R&D 상용화 자금을 지원합니다. 기업당 최대 1억원 이내 지원(매칭 20% 필수).',
'OPEN', 245, 'manager1', '2026-09-25 09:00:00'),

('대구디지털혁신진흥원(DIP) AI·SW 융합 솔루션 제작 바우처 지원사업', 'AI_SW', 800000000, 50000000, '2026-10-10', '2026-11-15',
'지역 중소제조업 및 전통산업에 인공지능(AI)과 빅데이터 솔루션을 접목하여 공정 혁신을 추진하는 기업을 지원합니다.',
'OPEN', 189, 'manager1', '2026-10-02 09:00:00'),

('2026년 하반기 디지털 헬스케어 실증 및 인허가 지원사업', 'HEALTH', 400000000, 40000000, '2026-10-12', '2026-11-20',
'웨어러블·원격모니터링 등 디지털 헬스케어 기기의 실증 및 인허가 비용을 지원합니다.',
'OPEN', 97, 'manager1', '2026-10-05 09:00:00'),

('2026년 스마트 제조혁신 AI 품질검사 도입 지원사업', 'SMART_FAB', 500000000, 50000000, '2026-10-15', '2026-11-27',
'제조 현장에 비전 AI 기반 자동 품질검사 시스템을 도입하는 기업을 지원합니다.',
'OPEN', 64, 'manager1', '2026-10-08 09:00:00');

-- 5. 신청서 샘플 (announcement 13 = 모빌리티 R&D, 14 = AI·SW 바우처)
--    seed 계획서 파일은 물리 파일이 없으므로 plan_file_id 는 NULL
INSERT INTO application (receipt_no, announcement_id, company_user_id, project_name, total_budget, request_grant, self_funding, summary, status, submitted_at) VALUES
('DPMS-00000001', 13, 'company1', '도심형 PBV 자율주행용 고정밀 조향센서 모듈 국산화 개발', 125000000, 100000000, 25000000,
'대구 국가산단 및 테크노폴리스 자율주행 실증단지 인프라와 연계하여 중소형 PBV에 탑재 가능한 0.05도 정밀도 조향각 센서 국산화 개발 및 신뢰성 인증 획득.',
'SELECTED', '2026-10-15 14:30:00'),

('DPMS-00000002', 14, 'company2', '북성로 공구골목 가공 공정 매칭을 위한 비전 AI 기반 도면 분석 솔루션', 62500000, 50000000, 12500000,
'2D CAD 및 스캔 도면의 형상 치수를 딥러닝 비전 알고리즘으로 자동 추출하여 최적의 북성로 가공 업체를 실시간 역경매 추천하는 지능형 솔루션.',
'EVALUATING', '2026-10-18 11:20:00'),

('DPMS-00000003', 14, 'company1', '차량 부품 외관검사 데이터 기반 AI 불량 예측 솔루션', 50000000, 40000000, 10000000,
'모빌리티 부품 생산라인의 외관검사 이미지를 축적·학습하여 불량 발생을 사전 예측하는 AI 솔루션 개발.',
'SUBMITTED', '2026-10-20 10:05:00');

-- 6. 평가 샘플
--    과제 1: 평가위원 2명 평가 완료 (평균 88.5점) / 과제 2: 2명 배정, 채점 대기
INSERT INTO evaluation (application_id, evaluator_user_id, quantitative_score, qualitative_score, total_score, eval_comment, status, assigned_at, evaluated_at) VALUES
(1, 'evaluator1', 38, 54, 92, '기술적 차별성과 대구 모빌리티 밸류체인과의 연계성이 매우 우수함. 사업화 성공 가능성이 높아 지원 타당함.', 'COMPLETED', '2026-10-16 09:00:00', '2026-10-19 16:00:00'),
(1, 'evaluator2', 35, 50, 85, '시장성은 충분하나 신뢰성 인증 일정이 다소 낙관적임. 인증 계획 보완 조건으로 지원 타당.', 'COMPLETED', '2026-10-16 09:00:00', '2026-10-19 17:30:00'),
(2, 'evaluator1', 0, 0, 0, NULL, 'ASSIGNED', '2026-10-19 10:00:00', NULL),
(2, 'evaluator2', 0, 0, 0, NULL, 'ASSIGNED', '2026-10-19 10:00:00', NULL);

-- 7. 사업비 정산 내역 샘플 (과제 1)
INSERT INTO settlement (application_id, item_code, expense_date, vendor_name, supply_amount, vat_amount, total_amount, description, status, reviewed_by, reviewed_at) VALUES
(1, 'EXP02', '2026-10-16', '(주)대구센서테크', 15000000, 1500000, 16500000, '센서 테스트보드 PCB 및 칩셋 샘플 구입', 'APPROVED', 'manager1', '2026-10-17 11:00:00'),
(1, 'EXP03', '2026-10-18', '북성로정밀금형', 22000000, 2200000, 24200000, '센서 외장 알루미늄 케이스 정밀 밀링 가공', 'REQUESTED', NULL, NULL);

-- 8. 공지사항 샘플
INSERT INTO notice (notice_id, title, content, important_yn, view_count, use_yn, writer_id, created_at) VALUES
('NTC_0000000001', '[필독] D-PMS 지원사업 포털 이용 안내',
'D-PMS 지원사업 포털은 공고 확인 → 온라인 신청 → 심사 → 선정 → 사업비 e-정산까지 한 곳에서 처리합니다.
신청 전 공고문의 지원 요건과 최대 지원금을 반드시 확인하시기 바랍니다.', 'Y', 128, 'Y', 'manager1', '2026-10-01 09:00:00'),
('NTC_0000000002', '2026년 하반기 사업비 정산 증빙 제출 기한 안내',
'협약 과제의 하반기 사업비 집행 증빙(세금계산서, 영수증)은 12월 15일까지 e-정산 메뉴에서 등록해 주시기 바랍니다.
기한 이후 등록분은 다음 연도 정산으로 이월될 수 있습니다.', 'Y', 64, 'Y', 'manager1', '2026-10-10 10:00:00'),
('NTC_0000000003', '시스템 정기 점검 안내 (10/25 02:00~04:00)',
'안정적인 서비스 제공을 위해 10월 25일(토) 02:00~04:00 정기 점검을 실시합니다. 점검 시간에는 접속이 제한됩니다.', 'N', 21, 'Y', 'admin', '2026-10-18 15:00:00');
