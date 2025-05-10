package com.onmoim.server.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onmoim.server.example.entity.TestEntity;
import com.onmoim.server.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByOauthIdAndProvider(String oauthId, String provider);

}
