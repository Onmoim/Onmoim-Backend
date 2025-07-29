package com.onmoim.server.chat.service.facade;

import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatUserDto;
import com.onmoim.server.chat.service.ChatMessageService;
import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.entity.GroupUser;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;
import com.onmoim.server.user.entity.User;
import com.onmoim.server.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 메시지 관련 비즈니스 로직을 담당하는 Facade
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageFacade {

	private final GroupUserQueryService groupUserQueryService;
	private final UserQueryService userQueryService;
	private final GroupQueryService groupQueryService;
	private final ChatMessageService chatMessageService;

	/**
	 * 메시지 전송
	 */
	@Transactional
	public void sendMessage(ChatMessageDto message, Long userId) {
		Long roomId = message.getRoomId();
		log.debug("messageDto : {}, sender : {}", message, userId);

		isExistsOrThrow(message.getGroupId());

		// 메시지에 인증된 사용자 ID 설정
		User user = userQueryService.findById(userId);
		Group group = groupQueryService.getById(message.getGroupId());
		GroupUser groupUser = groupUserQueryService.findById(group.getId(), userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_EXISTS_GROUP));

		message.setSenderId(userId);
		message.setChatUserDto(ChatUserDto.create(user, groupUser.isOwner()));

		// 메시지 전송 서비스 호출
		chatMessageService.sendUserMessage(message);
	}

	private void isExistsOrThrow(Long groupId) {
		groupQueryService.existsById(groupId);
	}

}

