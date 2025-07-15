package com.onmoim.server.chat.repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.onmoim.server.chat.domain.ChatSequence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RoomChatMessageId는 '{RoomId}+{SequenceValue}'로 구성됩니다. 방마다 고유한 순차적 ID를 통해 클라이언트가 메시지 순서 및 유실을 판단합니다.
 * RoomChatMessageIdGenerator 는 간단하게 ConcurrentHashMap으로 구성했습니다.
 * 추후에 분산환겨을 고려하여 Redis 나 다른 방법으로 개선하면 좋을 것 같습니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InMemoryRoomChatMessageIdGenerator implements RoomChatMessageIdGenerator {

	private final ChatSequenceRepository sequenceRepository;

	// 채팅방 ID를 키로, AtomicLong 시퀀스 카운터를 값으로 저장하는 맵
	private static final ConcurrentHashMap<Long, AtomicLong> SEQUENCE_MAP = new ConcurrentHashMap<>();

	@Override
	@Transactional
	public Long getSequence(Long roomId) {
		// 1. 메모리에서 시퀀스 값 조회 (없으면 새로 생성)
		AtomicLong sequenceCounter = SEQUENCE_MAP.computeIfAbsent(roomId, this::loadSequenceFromDb);

		// 2. 시퀀스 값 증가 (원자적 연산)
		long nextSequence = sequenceCounter.incrementAndGet();

		updateSequenceInDatabase(roomId, nextSequence);

		// 4. roomId와 시퀀스를 결합한 ID 반환
		return nextSequence;
	}

	/**
	 * DB에서 시퀀스 값을 로드하거나, 없을 경우 새로 생성
	 */
	private AtomicLong loadSequenceFromDb(Long roomId) {
		try {
			log.debug("메모리에 시퀀스 없음, roomId: {}, DB에서 조회", roomId);
			// DB에서 시퀀스 조회
			return sequenceRepository.findById(roomId)
				.map(sequence -> {
					log.debug("DB에서 시퀀스 로드, roomId: {}, 현재값: {}", roomId, sequence.getCurrentSequence());
					return new AtomicLong(sequence.getCurrentSequence());
				})
				.orElseGet(() -> {
					// 없으면 새로 생성 (시작값 0)
					log.debug("DB에 시퀀스 없음, roomId: {}, 새로 생성", roomId);
					ChatSequence newSequence = new ChatSequence(roomId, 0L);
					sequenceRepository.save(newSequence);
					return new AtomicLong(0);
				});
		} catch (Exception e) {
			log.error("DB에서 시퀀스 로드 실패, roomId: {}, 초기값 0으로 시작, 오류: {}", roomId, e.getMessage());
			return new AtomicLong(0);
		}
	}

	/**
	 * DB에 시퀀스 값 업데이트
	 */
	@Transactional
	public void updateSequenceInDatabase(Long roomId, Long sequence) {
		try {
			sequenceRepository.findById(roomId)
				.ifPresent(chatSequence -> {
					chatSequence.setCurrentSequence(sequence);
					sequenceRepository.save(chatSequence);
					log.debug("시퀀스 DB 업데이트 완료, roomId: {}, sequence: {}", roomId, sequence);
				});
		} catch (Exception e) {
			log.error("시퀀스 DB 업데이트 실패, roomId: {}, sequence: {}, 오류: {}",
				roomId, sequence, e.getMessage());
		}
	}
}