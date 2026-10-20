-- D-PMS Initial Seed Data
-- 1. 공통코드 그룹
INSERT INTO com_code_group (group_id, group_name, description, use_yn) VALUES
('FIELD', '지원 분야', '대구 신산업 R&D 및 기술지원 5대 전략 분야', 'Y'),
('APP_STATUS', '신청서 진행상태', '지원사업 신청부터 협약까지의 상태머신', 'Y'),
('EXP_ITEM', '사업비 비목분류', '보조금 관리규정에 따른 사업비 집행 비목', 'Y'),
('SETTLE_STATUS', '정산 검수상태', '사업비 영수증 검수 상태', 'Y');

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

-- 3. 테스트 계정 (비밀번호: 각 ID와 동일 뒤에 1234)
INSERT INTO users (user_id, password, user_name, email, phone, company_name, business_no, role, enabled) VALUES
('admin', '{noop}admin1234', '총괄관리자', 'admin@daegu.go.kr', '053-803-1000', '대구광역시청 미래혁신성장실', '504-83-00001', 'ROLE_ADMIN', TRUE),
('manager1', '{noop}manager1234', '김담당', 'manager1@dpg.or.kr', '053-602-1700', '대구디지털혁신진흥원(DIP)', '504-82-12345', 'ROLE_MANAGER', TRUE),
('evaluator1', '{noop}eval1234', '박심사', 'eval1@knu.ac.kr', '053-950-5555', '경북대학교 IT대학(평가위원)', '502-83-99999', 'ROLE_EVALUATOR', TRUE),
('company1', '{noop}company1234', '이대표', 'ceo@daegumobility.kr', '053-740-1234', '(주)대구모빌리티솔루션', '504-81-98765', 'ROLE_COMPANY', TRUE),
('company2', '{noop}company1234', '최기술', 'cto@suseongrobot.com', '053-750-5678', '(주)수성AI로보틱스', '504-86-54321', 'ROLE_COMPANY', TRUE);

-- 4. 지원사업 공고 샘플
INSERT INTO announcement (announcement_id, title, field_code, total_budget, max_grant, start_date, end_date, content, status, view_count, writer_id) VALUES
(1, '2026년 대구 미래 모빌리티 부품 R&D 및 상용화 지원사업 공고', 'MOBILITY', 1500000000, 100000000, '2026-10-01', '2026-11-30', 
'대구광역시 내 미래 모빌리티 관련 전장부품, 구동모터, 자율주행 센서 개발 기업을 대상으로 시제품 제작 및 R&D 상용화 자금을 지원합니다. 기업당 최대 1억원 이내 지원(매칭 20% 필수).', 
'OPEN', 245, 'manager1'),

(2, '대구디지털혁신진흥원(DIP) AI·SW 융합 솔루션 제작 바우처 지원사업', 'AI_SW', 800000000, 50000000, '2026-10-10', '2026-11-15', 
'지역 중소제조업 및 전통산업에 인공지능(AI)과 빅데이터 솔루션을 접목하여 공정 혁신을 추진하는 기업을 지원합니다.', 
'OPEN', 189, 'manager1'),

(3, '대구 로봇산업 혁신클러스터 청년 스타트업 스케일업 지원사업', 'ROBOT', 600000000, 70000000, '2026-09-01', '2026-10-15', 
'협동로봇 및 물류 자동화 로봇 시제품 제작을 위한 재료비 및 전문 가공비를 지원합니다.', 
'CLOSED', 312, 'manager1');

-- 5. 신청서 샘플
INSERT INTO application (application_id, announcement_id, company_user_id, project_name, total_budget, request_grant, self_funding, summary, plan_file_name, status, submitted_at) VALUES
(1, 1, 'company1', '도심형 PBV 자율주행용 고정밀 조향센서 모듈 국산화 개발', 125000000, 100000000, 25000000, 
'대구 국가산단 및 테크노폴리스 자율주행 실증단지 인프라와 연계하여 중소형 PBV에 탑재 가능한 0.05도 정밀도 조향각 센서 국산화 개발 및 신뢰성 인증 획득.', 
'daegu_mobility_plan_v1.pdf', 'SELECTED', '2026-10-15 14:30:00'),

(2, 2, 'company2', '북성로 공구골목 가공 공정 매칭을 위한 비전 AI 기반 도면 분석 솔루션', 62500000, 50000000, 12500000, 
'2D CAD 및 스캔 도면의 형상 치수를 딥러닝 비전 알고리즘으로 자동 추출하여 최적의 북성로 가공 업체를 실시간 역경매 추천하는 지능형 솔루션.', 
'suseong_robot_ai_plan.pdf', 'EVALUATING', '2026-10-18 11:20:00');

-- 6. 평가 샘플
INSERT INTO evaluation (evaluation_id, application_id, evaluator_user_id, quantitative_score, qualitative_score, total_score, eval_comment, status, evaluated_at) VALUES
(1, 1, 'evaluator1', 38, 54, 92, '기술적 차별성과 대구 모빌리티 밸류체인과의 연계성이 매우 우수함. 사업화 성공 가능성이 높아 지원 타당함.', 'COMPLETED', '2026-10-19 16:00:00');

-- 7. 사업비 정산 내역 샘플
INSERT INTO settlement (settlement_id, application_id, item_code, expense_date, vendor_name, supply_amount, vat_amount, total_amount, description, receipt_file_name, status) VALUES
(1, 1, 'EXP02', '2026-10-16', '(주)대구센서테크', 15000000, 1500000, 16500000, '센서 테스트보드 PCB 및 칩셋 샘플 구입', 'tax_invoice_20261016.pdf', 'APPROVED'),
(2, 1, 'EXP03', '2026-10-18', '북성로정밀금형', 22000000, 2200000, 24200000, '센서 외장 알루미늄 케이스 정밀 밀링 가공', 'tax_invoice_20261018.pdf', 'REQUESTED');
