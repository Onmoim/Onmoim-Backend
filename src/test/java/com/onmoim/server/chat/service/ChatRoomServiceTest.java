// package com.onmoim.server.chat.service;
//
// import static org.junit.jupiter.api.Assertions.*;
//
// import java.util.List;
//
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.transaction.annotation.Transactional;
//
// import com.onmoim.server.chat.dto.ChatRoomResponse;
// import com.onmoim.server.chat.entity.ChatRoom;
// import com.onmoim.server.chat.entity.ChatRoomMember;
// import com.onmoim.server.chat.repository.ChatRoomMemberRepository;
// import com.onmoim.server.chat.repository.ChatRoomRepository;
//
// @SpringBootTest
// @Transactional
// class ChatRoomServiceTest {
//
// 	@Autowired
// 	private ChatRoomService chatRoomService;
//
// 	@Autowired
// 	private ChatRoomRepository chatRoomRepository;
//
// 	@Autowired
// 	private ChatRoomMemberRepository chatRoomMemberRepository;
//
// 	@Test
// 	@DisplayName("채팅방 생성 및 멤버 추가 통합 테스트")
// 	void testCreateRoom() {
// 		// given
// 		String name = "Integration Test Room";
// 		String description = "Integration Test Description";
// 		Long creatorId = 1L;
//
// 		// when
// 		ChatRoomResponse result = chatRoomService.createRoom(name, description, creatorId);
//
// 		// then
// 		// 채팅방 검증
// 		assertNotNull(result);
// 		assertNotNull(result.getId());
// 		assertEquals(name, result.getName());
// 		assertEquals(description, result.getDescription());
// 		assertEquals(creatorId, result.getCreatorId());
//
// 		// 데이터베이스에서 채팅방 조회 및 검증
// 		ChatRoom savedRoom = chatRoomRepository.findById(result.getId()).orElse(null);
// 		assertNotNull(savedRoom);
// 		assertEquals(name, savedRoom.getName());
// 		assertEquals(description, savedRoom.getDescription());
// 		assertEquals(creatorId, savedRoom.getCreatorId());
//
// 		// 채팅방 멤버 검증
// 		List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(result.getId());
// 		assertEquals(1, members.size());
// 		assertEquals(creatorId, members.get(0).getUserId());
// 		assertEquals(result.getId(), members.get(0).getChatRoom().getId());
// 	}
// }