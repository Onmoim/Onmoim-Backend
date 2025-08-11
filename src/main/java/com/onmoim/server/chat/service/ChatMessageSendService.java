package com.onmoim.server.chat.service;

import static com.onmoim.server.chat.domain.enums.SubscribeRegistry.*;

import java.util.List;

import com.onmoim.server.chat.domain.ChatRoomMessageId;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatRoomListUpdateDto;
import com.onmoim.server.chat.domain.enums.DeliveryStatus;
import com.onmoim.server.chat.service.retry.ChatMessageRetryService;
import com.onmoim.server.group.dto.GroupMember;
import com.onmoim.server.group.entity.Group;
import com.onmoim.server.group.implement.GroupQueryService;
import com.onmoim.server.group.implement.GroupUserQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageSendService {
	private final SimpMessagingTemplate messagingTemplate;
	private final ChatMessageRetryService chatMessageRetryService;
	private final ChatStatusService chatStatusService;
	private final GroupUserQueryService groupUserQueryService;
	private final GroupQueryService groupQueryService;

	@Transactional
	public void send(String destination, ChatMessageDto message) {
		messageSend(destination, message);
		updateChatRoomList(message);
	}

	public void messageSend(String destination, ChatMessageDto message) {
		ChatRoomMessageId messageId = ChatRoomMessageId.create(message.getGroupId(), message.getMessageSequence());

		try {
			// WebSocket을 통해 메시지 전송
			messagingTemplate.convertAndSend(destination, message);

			// 전송 성공 시 SENT 상태 업데이트
			chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.SENT);
			log.debug("메시지 전송 완료: ID: {}, 방ID: {}", messageId, message.getGroupId());

		} catch (Exception e) {
			// 전송 실패 시 FAILED 상태 업데이트
			chatStatusService.updateMessageDeliveryStatus(messageId, DeliveryStatus.FAILED);
			log.warn("메시지 전송 실패: ID: {}, 방ID: {}, 오류: {}", messageId, message.getGroupId(), e.getMessage());

			// 실패 재시도 처리
			chatMessageRetryService.failedProcess(message, destination);
		}
	}

	/**
	 * 채팅방 참여자들의 채팅방 목록 업데이트
	 */
	public void updateChatRoomList(ChatMessageDto latestMessage) {
		Long roomId = latestMessage.getGroupId();
		try {
			// 해당 채팅방에 참여한 모든 사용자 조회
			int size = 100;
			List<GroupMember> roomUsers = groupUserQueryService.findGroupUserAndMembers(roomId, null, size);
			Group group = groupQueryService.getById(roomId);
			Long count = groupUserQueryService.countMembers(roomId);

			// 채팅방 목록 업데이트용 DTO 생성
			ChatRoomListUpdateDto updateDto = ChatRoomListUpdateDto.builder()
				.groupId(roomId)
				.groupName(group.getName())
				.participantCount(count)
				.latestMessage(latestMessage)
				.build();

			// 각 참여자의 채팅방 목록 토픽으로 전송
			for (GroupMember groupMember : roomUsers) {
				Long userId = groupMember.memberId();
				String destination = CHAT_ROOM_LIST_PREFIX.getDestination() + userId;

				try {
					messagingTemplate.convertAndSend(destination, updateDto);
					log.debug("채팅방 목록 업데이트 전송: 사용자ID: {}, 방ID: {}", userId, roomId);

				} catch (Exception e) {
					log.warn("채팅방 목록 업데이트 실패: 사용자ID: {}, 방ID: {}, 오류: {}",
						userId, roomId, e.getMessage());
				}
			}

			log.info("채팅방 목록 업데이트 완료: 방ID: {}, 대상 사용자 수: {}", roomId, roomUsers.size());

		} catch (Exception e) {
			log.error("채팅방 목록 업데이트 실패: 방ID: {}, 오류: {}", roomId, e.getMessage(), e);
		}
	}
}
