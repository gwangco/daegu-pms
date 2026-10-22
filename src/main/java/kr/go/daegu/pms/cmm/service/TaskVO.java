package kr.go.daegu.pms.cmm.service;

/**
 * 메인 화면 "처리할 일" 카드 한 장.
 *
 * @param label  업무 이름 (예: 서류 검토 대기)
 * @param count  건수
 * @param url    눌렀을 때 이동할 목록
 * @param alert  건수가 있으면 강조할지 (예: 보완 요청, 잠긴 계정)
 */
public record TaskVO(String label, int count, String url, boolean alert) {
}
