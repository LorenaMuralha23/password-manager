package com.tcc.password_manager.service;

import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.repository.PasswordReferenceRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PasswordReferenceService {

    private final PasswordReferenceRepository passwordReferenceRepository;

    public PasswordReferenceService(PasswordReferenceRepository passwordReferenceRepository) {
        this.passwordReferenceRepository = passwordReferenceRepository;
    }

    public PasswordReference save(PasswordReference reference) {
        return passwordReferenceRepository.save(reference);
    }

    public List<PasswordReference> findAll() {
        return passwordReferenceRepository.findAll();
    }

    public Optional<PasswordReference> findById(Long id) {
        return passwordReferenceRepository.findById(id);
    }

    public List<PasswordReference> findByUserId(Long userId) {
        return passwordReferenceRepository.findByUserId(userId);
    }

    public void delete(Long id) {
        passwordReferenceRepository.deleteById(id);
    }
}
