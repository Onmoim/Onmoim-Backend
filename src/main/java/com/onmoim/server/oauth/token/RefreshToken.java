package com.onmoim.server.oauth.token;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

	@Id
	private Long userId;

	@Column(nullable = false, length = 512)
	private String token;

	@Column(nullable = false)
	private LocalDateTime expiration;

}
