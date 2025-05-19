package com.onmoim.server.common;

import java.time.LocalDateTime;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {
    // Entity가 생성되어 저장될 때 시간이 자동 저장됩니다.
    @CreatedDate
    private LocalDateTime createdDate;

    // 조회한 Entity 값을 변경할 때 시간이 자동 저장됩니다.
    @LastModifiedDate
    private LocalDateTime modifiedDate;

    // 삭제 시점
    private LocalDateTime deletedDate;

    /**
     * 소프트 삭제 처리
     */
    public void softDelete() {
        this.deletedDate = LocalDateTime.now();
    }

	// 삭제 시점 추가
	private LocalDateTime deletedDate;

	// soft delete
	protected final void delete() {
		deletedDate = LocalDateTime.now();
	}

	// soft delete 삭제 확인
	protected final boolean isActive() {
		return deletedDate == null;
	}

    /**
     * 삭제 여부 확인
     */
    public boolean isDeleted() {
        return deletedDate != null;
    }

}
