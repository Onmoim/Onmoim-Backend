package com.onmoim.server.chat.repository;

import static com.onmoim.server.category.entity.QCategory.*;
import static com.onmoim.server.group.entity.QGroup.*;
import static com.onmoim.server.group.entity.QGroupLike.*;
import static com.onmoim.server.group.entity.QGroupUser.*;
import static com.onmoim.server.location.entity.QLocation.*;

import java.time.LocalDateTime;
import java.util.List;

import com.onmoim.server.chat.domain.QChatRoomMessage;
import com.onmoim.server.chat.domain.dto.ChatMessageDto;
import com.onmoim.server.chat.domain.dto.ChatRoomSummeryDto;
import com.onmoim.server.group.entity.GroupLikeStatus;
import com.onmoim.server.group.entity.QGroupUser;
import com.onmoim.server.group.entity.Status;
import com.onmoim.server.meeting.entity.QMeeting;
import com.onmoim.server.user.entity.QUser;  // 가정: 유저 엔티티 Q클래스
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ChatRoomRepositoryCustomImpl implements ChatRoomRepositoryCustom {
	private final JPAQueryFactory queryFactory;

	public List<ChatRoomSummeryDto> findJoinedGroupList(Long userId, LocalDateTime cursorTime, String cursorGroupName, int size) {

		QGroupUser groupUserSub = new QGroupUser("groupUserSub");
		QMeeting meetingSub = new QMeeting("meetingSub");
		QChatRoomMessage chatMessageSub = new QChatRoomMessage("chatMessageSub");
		QUser sender = new QUser("sender");  // 유저 정보 조인용 별칭

		BooleanBuilder where = new BooleanBuilder();
		where.and(groupUser.user.id.eq(userId));
		where.and(groupUser.status.in(Status.OWNER, Status.MEMBER));

		// 마지막 메시지 생성 시간 서브쿼리
		Expression<LocalDateTime> lastMsgTimeExpr = JPAExpressions
			.select(chatMessageSub.createdDate)
			.from(chatMessageSub)
			.where(
				chatMessageSub.id.roomId.eq(group.id),
				chatMessageSub.id.messageSequence.eq(
					JPAExpressions.select(chatMessageSub.id.messageSequence.max())
						.from(chatMessageSub)
						.where(chatMessageSub.id.roomId.eq(group.id))
				)
			);

		// 커서 페이징 조건 (lastMsgTime < cursorTime) OR (lastMsgTime = cursorTime AND groupName > cursorGroupName)
		if (cursorTime != null && cursorGroupName != null) {
			where.and(
				Expressions.booleanTemplate(
					"({0} < {1}) OR ({0} = {1} AND {2} > {3})",
					lastMsgTimeExpr, cursorTime,
					group.name, cursorGroupName
				)
			);
		}

		// 멤버 수 서브쿼리
		Expression<Long> memberCountExpr = JPAExpressions
			.select(groupUserSub.id.countDistinct())
			.from(groupUserSub)
			.where(
				groupUserSub.group.id.eq(group.id),
				groupUserSub.status.in(Status.OWNER, Status.MEMBER)
			);

		// 예정된 미팅 수 서브쿼리
		Expression<Long> upcomingMeetingCountExpr = JPAExpressions
			.select(meetingSub.id.countDistinct())
			.from(meetingSub)
			.where(
				meetingSub.group.id.eq(group.id),
				meetingSub.startAt.gt(LocalDateTime.now())
			);

		// 마지막 메시지 전체 정보 프로젝션
		Expression<ChatMessageDto> lastMessageDtoExpr = Projections.constructor(
			ChatMessageDto.class,
			chatMessageSub.id.messageSequence,
			chatMessageSub.id.roomId,
			chatMessageSub.type,
			chatMessageSub.content,
			chatMessageSub.senderId,
			chatMessageSub.createdDate,
			sender.id,
			sender.name,
			sender.profileImgUrl
		);

		// 정렬 기준: 마지막 메시지 시간 DESC nullsLast + 그룹 이름 ASC
		OrderSpecifier<LocalDateTime> lastMsgOrder = new OrderSpecifier<>(
			Order.DESC,
			lastMsgTimeExpr,
			OrderSpecifier.NullHandling.NullsLast
		);
		OrderSpecifier<String> groupNameOrder = new OrderSpecifier<>(
			Order.ASC,
			group.name,
			OrderSpecifier.NullHandling.Default
		);

		List<ChatRoomSummeryDto> result = queryFactory
			.select(Projections.constructor(
				ChatRoomSummeryDto.class,
				group.id,
				group.name,
				group.imgUrl,
				category.name,
				groupUser.status,
				new CaseBuilder()
					.when(groupLike.status.eq(GroupLikeStatus.LIKE)).then("LIKE")
					.otherwise("NONE"),
				lastMessageDtoExpr,
				location.dong,
				memberCountExpr,
				upcomingMeetingCountExpr
			))
			.from(groupUser)
			.leftJoin(groupUser.group, group)
			.leftJoin(group.category, category)
			.leftJoin(group.location, location)
			.leftJoin(groupLike).on(
				groupLike.user.eq(groupUser.user),
				groupLike.group.eq(groupUser.group)
			)
			// 마지막 메시지 조인 (서브쿼리 기반으로 최대 messageSequence 일치 조건)
			.leftJoin(chatMessageSub).on(
				chatMessageSub.id.roomId.eq(group.id)
					.and(chatMessageSub.id.messageSequence.eq(
						JPAExpressions.select(chatMessageSub.id.messageSequence.max())
							.from(chatMessageSub)
							.where(chatMessageSub.id.roomId.eq(group.id))
					))
			)
			// 발신자 유저 정보 조인
			.leftJoin(sender).on(chatMessageSub.senderId.eq(sender.id))
			.where(where)
			.orderBy(lastMsgOrder, groupNameOrder)
			.limit(size + 1)
			.fetch();

		return result;
	}

	public LocalDateTime getLastMessageTime(Long groupId) {
		QChatRoomMessage chatMessage = new QChatRoomMessage("chatMessage");

		return queryFactory
			.select(chatMessage.createdDate)
			.from(chatMessage)
			.where(
				chatMessage.id.roomId.eq(groupId),
				chatMessage.id.messageSequence.eq(
					JPAExpressions.select(chatMessage.id.messageSequence.max())
						.from(chatMessage)
						.where(chatMessage.id.roomId.eq(groupId))
				)
			)
			.fetchOne();
	}
}
