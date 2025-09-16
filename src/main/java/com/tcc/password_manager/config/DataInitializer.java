package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.service.AppUserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(AppUserService appUserService) {

        return args -> {
            ObjectMapper mapper = new ObjectMapper();

            // === Cadastro de usuário ===
            String senhaMestre = "senha123"; // senha fornecida pelo usuário
            String mfaSecret = "mfa1";       // no futuro, segredo TOTP

            AppUser user = appUserService.registerUser(senhaMestre, mfaSecret);

            System.out.println("Entity salva no banco (User): " + user.getEncryptedData());

            // === Recuperar DTO interno para validação ===
            // Obs: aqui precisaríamos do fluxo de login para recuperar a UMK.
            // Por enquanto vamos só confirmar que a entity foi criada.
            System.out.println("Usuário de teste inicializado com dados cifrados!");
        };
    }
}
