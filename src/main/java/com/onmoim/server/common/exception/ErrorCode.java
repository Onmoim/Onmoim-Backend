package com.onmoim.server.common.exception;

import static org.springframework.http.HttpStatus.*;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@AllArgsConstructor
@Slf4j
public enum ErrorCode {

	/* ------------------ 400 BAD_REQUEST : 잘못된 요청 ------------------ */

	INVALID_LOGIN_INFO(BAD_REQUEST, "아이디 또는 비밀번호가 일치하지 않습니다."),
	FILE_UPLOAD_FAILED(BAD_REQUEST, "파일 업로드에 실패했습니다."),
	FILE_DELETE_FAILED(BAD_REQUEST, "파일 삭제에 실패했습니다."),
	EMPTY_FILE(BAD_REQUEST, "파일이 비어 있습니다."),
	INVALID_FILE_TYPE(BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
	FILE_SIZE_EXCEEDED(BAD_REQUEST, "파일 크기가 허용된 최대 크기를 초과했습니다."),
	IMAGE_COUNT_EXCEEDED(BAD_REQUEST, "이미지는 최대 5개까지만 업로드 가능합니다."),
	INVALID_USER(BAD_REQUEST, "잘못된 사용자입니다."),

	INVALID_LOCATION(BAD_REQUEST, "잘못된 위치 정보입니다."),
	INVALID_CATEGORY(BAD_REQUEST, "잘못된 카테고리입니다."),

	FILE_NOT_FOUND(BAD_REQUEST, "삭제할 이미지가 이미 존재하지 않습니다."),

	INVALID_KAKAO_RESPONSE(BAD_REQUEST, "카카오 사용자 정보 응답이 올바르지 않습니다."),
	NOT_FOUND_MESSAGE(BAD_REQUEST, "잘못된 MessageID 입니다."),
	IS_NOT_CHAT_ROOM_MEMBER(BAD_REQUEST, "채팅방 멤버가 아닌 사용자입니다."),

	/* ------------------ 400 BAD_REQUEST : 유저 관련 오류 ------------------ */
	ALREADY_EXISTS_USER(BAD_REQUEST, "이미 가입된 사용자입니다."),

	/* ------------------ 4XX BAD_REQUEST : 모임 관련 오류 ------------------ */
	NOT_EXISTS_GROUP(NOT_FOUND, "존재하지 않는 모임입니다."),
	ALREADY_EXISTS_GROUP(CONFLICT, "이미 존재하는 이름의 모임입니다."),
	GROUP_ALREADY_JOINED(BAD_REQUEST, "이미 모임에 가입된 회원입니다."),
	GROUP_BANNED_MEMBER(BAD_REQUEST, "모임에서 차단된 회원입니다."),
	GROUP_CAPACITY_EXCEEDED(CONFLICT, "정원이 이미 꽉 찬 모임입니다."),
	GROUP_FORBIDDEN(FORBIDDEN, "권한이 부족합니다."),
	GROUP_OWNER_TRANSFER_REQUIRED(CONFLICT, "모임장은 모임장을 넘기고 탈퇴할 수 있습니다."),
	MEMBER_NOT_FOUND_IN_GROUP(NOT_FOUND, "모임 멤버를 찾을 수 없습니다."),
	CAPACITY_MUST_BE_GREATER_THAN_CURRENT(CONFLICT,"정원은 현재 모임 수 이상이어야 합니다."),
	MAP_API_ERROR(INTERNAL_SERVER_ERROR, "카카오 맵 API 문제"),

	/* ------------------ 400 BAD_REQUEST : 일정 관련 오류 ------------------ */
	MEETING_NOT_FOUND(BAD_REQUEST, "존재하지 않는 일정입니다."),
	MEETING_UPDATE_FORBIDDEN(FORBIDDEN, "일정 수정 권한이 없습니다."),
	MEETING_UPDATE_TIME_EXCEEDED(BAD_REQUEST, "일정 시작 24시간 전까지만 수정 가능합니다."),
	MEETING_CAPACITY_EXCEEDED(CONFLICT, "일정 정원이 가득 찼습니다."),
	MEETING_ALREADY_JOINED(BAD_REQUEST, "이미 참석 신청한 일정입니다."),
	MEETING_NOT_JOINED(BAD_REQUEST, "참석하지 않은 일정입니다."),
	MEETING_ALREADY_CLOSED(BAD_REQUEST, "이미 종료된 일정입니다."),
	MEETING_CAPACITY_CANNOT_REDUCE(BAD_REQUEST, "현재 참석 인원보다 적게 설정할 수 없습니다."),

	/* ------------------ 400 BAD_REQUEST : 게시글 관련 오류 ------------------ */
	POST_NOT_FOUND(BAD_REQUEST, "게시글을 찾을 수 없습니다."),

	/* ------------------ 401 BAD_REQUEST : 권한 없음 ------------------ */
	DENIED_UNAUTHORIZED_USER(UNAUTHORIZED, "로그인되지 않은 유저의 접근입니다."),

	/* ------------------ 429 TOO_MANY_REQUEST : 너무 많은 요청 ------------------ */
	TOO_MANY_REQUEST(TOO_MANY_REQUESTS, "잠시 후 다시 시도해 주세요"),

	/* ------------------ 429 TOO_MANY_REQUESTS : 과도한 요청 ------------------ */
	MEETING_LOCK_TIMEOUT(HttpStatus.TOO_MANY_REQUESTS, "다른 사용자가 처리 중입니다. 잠시 후 다시 시도해 주세요"),

	/* ------------------ 500 INTERNAL_SERVER_ERROR : 시스템 오류 ------------------ */
	LOCK_SYSTEM_ERROR(INTERNAL_SERVER_ERROR, "락 시스템 오류가 발생했습니다. 잠시 후 다시 시도해 주세요"),

	/* ------------------ 400 BAD_REQUEST : 권한 관련 오류 ------------------ */
	NOT_GROUP_MEMBER(FORBIDDEN, "모임 멤버만 접근 가능합니다."),

	/* ------------------ 401 UNAUTHORIZED : Auth 관련 오류 ------------------ */
	INVALID_REFRESH_TOKEN(UNAUTHORIZED, "유효하지 않은 refresh token입니다."),
	REFRESH_TOKEN_MISMATCH(UNAUTHORIZED, "저장된 refresh token과 일치하지 않습니다."),
	USER_NOT_FOUND(UNAUTHORIZED, "존재하지 않는 사용자입니다."),
	UNAUTHORIZED_ACCESS(UNAUTHORIZED, "인증되지 않은 사용자입니다.");


	private final HttpStatus httpStatus;
	private final String detail;
}
