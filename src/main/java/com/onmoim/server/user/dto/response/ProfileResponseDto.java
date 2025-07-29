package com.onmoim.server.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.location.entity.Location;

import com.onmoim.server.user.entity.UserCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "유저 프로필 response")
public class ProfileResponseDto {

	@Schema(description = "유저 id")
	private Long id;

	@Schema(description = "이름")
	private String name;

	@Schema(description = "성별")
	private String gender;

	@Schema(description = "지역 id")
	private Long locationId;

	@Schema(description = "지역명")
	private String locationName;

	@Schema(description = "생년월일")
	private LocalDateTime birth;

	@Schema(description = "자기소개")
	private String introduction;

//	@Schema(description = "카테고리 id 리스트")
//	private List<Long> categoryIdList;
//
//	@Schema(description = "카테고리명 리스트")
//	private List<String> categoryNameList;

	@Schema(description = "카테고리 리스트")
	private List<UserCategoryResponseDto> categoryList;

	@Schema(description = "찜 모임 개수")
	private Long likedGroupsCount;

	@Schema(description = "최근 본 모임 개수")
	private Long recentViewedGroupsCount;

	@Schema(description = "가입한 모임 개수")
	private Long joinedGroupsCount;

	@Schema(description = "프로필 사진 url")
	private String profileImgUrl;

}
