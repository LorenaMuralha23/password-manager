package com.tcc.password_manager.config;

import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.repository.AppUserRepository;
import com.tcc.password_manager.repository.PasswordReferenceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(AppUserRepository userRepository,
            PasswordReferenceRepository passwordRepository) {
        return args -> {
            // Criando usuários
            AppUser user1 = new AppUser(null, "mfa1", "symKey1", "hash1");
            AppUser user2 = new AppUser(null, "mfa2", "symKey2", "hash2");

            userRepository.save(user1);
            userRepository.save(user2);

            // Criando senhas vinculadas ao user1
            PasswordReference ref1 = new PasswordReference(null, "Email", "bcRef1", "bcRef2", "frag1");
            ref1.setUser(user1);

            PasswordReference ref2 = new PasswordReference(null, "Banco", "bcRef3", "bcRef4", "frag2");
            ref2.setUser(user1);

            passwordRepository.save(ref1);
            passwordRepository.save(ref2);
        };
    }
}
