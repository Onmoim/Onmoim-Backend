package com.onmoim.server.chat.entity;

public enum DeliveryStatus {
	PENDING,            // 전송 대기 중
	SENT,               // 메시지 전송 완료
	FAILED,             // 전송 실패 (재시도 예정)
	FAILED_PERMANENTLY  // 최대 재시도 횟수 초과 후 최종 실패
}
