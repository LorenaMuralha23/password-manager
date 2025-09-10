package com.tcc.password_manager.repository;

import com.tcc.password_manager.model.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByMfaSecret(String mfaSecret);
}
