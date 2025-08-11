package com.onmoim.server.chat.controller.rest;

import static com.onmoim.server.common.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatRoomListUpdateDto;
import com.onmoim.server.chat.domain.dto.ChatRoomSummeryDto;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.chat.service.ChatRoomListService;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.response.ResponseHandler;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.security.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
@Tag(name = "chat", description = "채팅 관련 API")
public class ChatMessageController {
	private final ChatMessageService chatMessageService;
	private final ChatRoomListService chatRoomListService;

	@Operation(
		summary = "채팅방 메시지 목록 조회",
		description = "특정 채팅방의 메시지를 커서 기반으로 페이징 조회합니다. " +
			"커서(cursor)가 없으면 최신 메시지 100개를 반환합니다. " +
			"커서를 전달하면 해당 커서보다 이전 메시지 100개를 반환합니다.",
		parameters = {
			@Parameter(name = "roomId", description = "채팅방 ID", required = true, example = "1"),
			@Parameter(name = "cursor", description = "이전 메시지 커서 (messageSequence). " +
				"생략하면 최신 메시지 100개 조회", required = false, example = "12345")
		},
		responses = {
			@ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공",
				content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatRoomMessage.class)))),
			@ApiResponse(responseCode = "400", description = "잘못된 요청"),
			@ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
		}
	)
	@GetMapping("/chatrooms/{roomId}/messages")
	public ResponseEntity<ResponseHandler<List<ChatMessageDto>>> getMessages(
		@PathVariable Long roomId,
		@RequestParam(required = false) Long cursor) {
		List<ChatMessageDto> messages = chatMessageService.getMessages(roomId, cursor);
		return ResponseEntity.ok(ResponseHandler.response(messages));
	}

	/**
	 * 채팅방 목록 조회
	 * GET /api/chat/rooms
	 */

	@GetMapping("/chat/rooms")
	@Operation(
		summary = "채팅방 목록 조회",
		description = "사용자가 참여한 채팅방 목록을 마지막 메시지 시간과 그룹 이름 기준으로 커서 페이징 조회합니다.",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "채팅방 목록 조회 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = ResponseHandler.class) // ResponseHandler<List<ChatRoomSummeryDto>>로 실제 타입 설정
				)
			)
		}
	)
	public ResponseEntity<ResponseHandler<List<ChatRoomSummeryDto>>> getChatRoomList(
		@Parameter(description = "커서 기준 메시지 시간, 없으면 첫 페이지 조회", example = "2025-08-11T14:00:00")
		@RequestParam(required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
		LocalDateTime cursorTime,

		@Parameter(description = "커서 그룹 이름 시간이 같을 경우 사용 required = false ", example = "Alpha Group")
		@RequestParam(required = false)
		String cursorGroupName,

		@Parameter(description = "페이지 크기, 기본값 20", example = "20")
		@RequestParam(defaultValue = "20")
		int size
	) {
		Long userId = getCurrentUserId();
		List<ChatRoomSummeryDto> chatRooms = chatRoomListService.getChatRoomList(userId, cursorTime, cursorGroupName, size);
		return ResponseEntity.ok(ResponseHandler.response(chatRooms));
	}

	private Long getCurrentUserId() {
		Object principal = SecurityContextHolder.getContextHolderStrategy()
			.getContext()
			.getAuthentication()
			.getPrincipal();

		if ((principal instanceof CustomUserDetails customUserDetails)) {
			return customUserDetails.getUserId();
		}

		throw new CustomException(UNAUTHORIZED_ACCESS);
	}
}
