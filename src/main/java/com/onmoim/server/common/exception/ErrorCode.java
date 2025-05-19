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
	INVALID_USER(BAD_REQUEST,	"잘못된 사용자입니다."),
	INVALID_LOCATION(BAD_REQUEST, "잘못된 위치 정보입니다."),
	INVALID_CATEGORY(BAD_REQUEST, "잘못된 카테고리입니다."),

	/* ------------------ 400 BAD_REQUEST : 모임 잘못된 요청 ------------------ */
	NOT_EXISTS_GROUP(BAD_REQUEST, "존재하지 않는 모임입니다."),
	ALREADY_EXISTS_GROUP(BAD_REQUEST, "이미 존재하는 이름의 모임입니다."),
	GROUP_ALREADY_JOINED(BAD_REQUEST, "이미 모임에 가입된 회원입니다."),
	GROUP_BANNED_MEMBER(BAD_REQUEST, "모임에서 차단된 회원입니다."),
	GROUP_CAPACITY_EXCEEDED(BAD_REQUEST, "정원이 가득 찼습니다."),
	GROUP_FORBIDDEN(BAD_REQUEST, "권한이 부족합니다."),
	GROUP_OWNER_TRANSFER_REQUIRED(BAD_REQUEST, "모임장은 모임장을 넘기고 탈퇴할 수 있습니다."),

	/* ------------------ 401 BAD_REQUEST : 권한 없음 ------------------ */
	DENIED_UNAUTHORIZED_USER(UNAUTHORIZED, "로그인되지 않은 유저의 접근입니다."),

	/* ------------------ 409 CONFLICT : 권한 없음 ------------------ */
	TOO_MANY_REQUESTS(CONFLICT, "잠시 후 다시 시도해 주세요");

	private final HttpStatus httpStatus;
	private final String detail;
}
