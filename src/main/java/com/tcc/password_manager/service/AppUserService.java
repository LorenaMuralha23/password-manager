package com.tcc.password_manager.service;

import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.repository.AppUserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser save(AppUser user) {
        return userRepository.save(user);
    }

    public List<AppUser> findAll() {
        return userRepository.findAll();
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
