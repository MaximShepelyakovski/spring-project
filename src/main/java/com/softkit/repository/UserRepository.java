package com.softkit.repository;

import com.softkit.model.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    User findByUsername(String username);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByVerificationCode(String verificationCode);
    User findByVerificationCode(String verificationCode);
    Long countByEnabledIsTrue();
    User findByEmail(String usernameOrEmail);
    User save(User user);
}
