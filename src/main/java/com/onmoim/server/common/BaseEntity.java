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

    // soft delete
    public void softDelete() {
        this.deletedDate = LocalDateTime.now();
    }

    // soft delete 복구
    public void restore() {
        this.deletedDate = null;
    }

    // 삭제 확인
    public boolean isDeleted() {
        return deletedDate != null;
    }
}
