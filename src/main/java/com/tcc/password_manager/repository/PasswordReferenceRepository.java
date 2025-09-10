package com.tcc.password_manager.repository;

import com.tcc.password_manager.model.PasswordReference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordReferenceRepository extends JpaRepository<PasswordReference, Long> {
    List<PasswordReference> findByUserId(Long userId);
}
