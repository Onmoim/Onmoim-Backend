package com.onmoim.server.group.implement;

import com.onmoim.server.common.exception.CustomException;
import com.onmoim.server.common.exception.ErrorCode;
import com.onmoim.server.common.response.CommonCursorPageResponseDto;
import com.onmoim.server.group.dto.response.GroupSummaryResponseDto;
import com.onmoim.server.group.repository.GroupLikeRepository;
import com.onmoim.server.group.repository.GroupRepository;
import com.onmoim.server.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupLikeQueryService {

	private final GroupLikeRepository groupLikeRepository;
	private final GroupRepository groupRepository;

	/**
	 * 찜한 모임 조회
	 */
	public CommonCursorPageResponseDto<GroupSummaryResponseDto> getLikedGroups(Long cursorId, int size) {
		Long userId = getCurrentUserId();

		List<GroupSummaryResponseDto> result = groupLikeRepository.findLikedGroupList(userId, cursorId, size);

		if (result.isEmpty()) {
			return CommonCursorPageResponseDto.empty();
		}

		boolean hasNext = result.size() > size;
		List<GroupSummaryResponseDto> content = hasNext ? result.subList(0, size) : result;

		// 추천 여부 삽입
		Set<Long> recommendedGroupIds = groupRepository.findRecommendedGroupIds(userId);

		for (GroupSummaryResponseDto dto : content) {
			if (recommendedGroupIds.contains(dto.getGroupId())) {
				dto.setRecommendStatus("RECOMMEND");
			} else {
				dto.setRecommendStatus("NONE");
			}
		}

		Long nextCursorId = hasNext ? content.get(content.size() - 1).getGroupId() : null;

		return CommonCursorPageResponseDto.of(content, hasNext, nextCursorId);
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
