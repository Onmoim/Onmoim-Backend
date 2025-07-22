package com.onmoim.server.group.implement;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupLikeQueryService {

	private final GroupLikeRepository groupLikeRepository;

	/**
	 * 찜한 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getLikedGroups(Long cursorId, int size) {
		Long userId = getCurrentUserId();

		return groupLikeRepository.findLikedGroupListByUserId(userId, cursorId, size);
	}

	public Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}

		Object principal = auth.getPrincipal();

		if (principal instanceof CustomUserDetails userDetails) {
			return userDetails.getUserId();
		} else {
			throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
		}
	}
}
