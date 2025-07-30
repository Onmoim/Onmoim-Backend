package com.onmoim.server.chat.controller.rest;

import com.onmoim.server.chat.domain.ChatRoomMessage;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.common.response.ResponseHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "chat", description = "채팅 관련 API")
public class ChatMessageController {
	private final ChatMessageService chatMessageService;

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
	public ResponseEntity<ResponseHandler<List<ChatRoomMessage>>> getMessages(
		@PathVariable Long roomId,
		@RequestParam(required = false) Long cursor) {
		List<ChatRoomMessage> messages = chatMessageService.getMessages(roomId, cursor);
		return ResponseEntity.ok(ResponseHandler.response(messages));
	}
}
