package com.onmoim.server.chat.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.onmoim.server.chat.dto.ChatRoomResponse;
import com.onmoim.server.chat.dto.CreateRoomRequest;
import com.onmoim.server.chat.service.ChatRoomService;
import com.onmoim.server.common.response.ResponseHandler;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅방 관련 비즈니스 로직을 담당하는 Facade
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatRoomRestController {

	private final ChatRoomService chatRoomService;

	/**
	 * 채팅방 생성
	 */
	@PostMapping("/api/v1/chat/room")
	public ResponseEntity<?> createRoom(
		@Valid @RequestBody CreateRoomRequest request, Principal principal) {
		log.debug("ChatRoomRestController.createRoom");
		String userId = principal.getName();
		ChatRoomResponse roomDto = chatRoomService.createRoom(request.getName(), request.getDescription(), userId);

		return ResponseEntity.status(HttpStatus.CREATED).body(ResponseHandler.response(roomDto));
	}
}
