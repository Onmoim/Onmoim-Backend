package com.onmoim.server.group.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.onmoim.server.user.repository.UserRepository;

/**
 * P1 User Birth 필드: Date -> LocalDateTime 변경
 * P2 nullable 체크 애플리케이션 레벨에서 하는게 어떻나? 테스트 때 너무 힘들어욧
 * 논의 이후 테스트 작성 예정
 */
@SpringBootTest
class GroupServiceTest {
	@Autowired
	private GroupService groupService;
	@Autowired
	private UserRepository userRepository;
}
