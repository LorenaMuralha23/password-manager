package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.dto.AppUserClearData;
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

            System.out.println("=== Cadastro ===");
            System.out.println("Entity salva no banco (User): " + user.getEncryptedData());
            System.out.println("umkWrapped salvo: " + user.getUmkWrapped());
            System.out.println("umkHash salvo: " + user.getUmkHash());

            // === Simulando login ===
            AppUserClearData clearData = appUserService.login(user.getId(), senhaMestre);

            System.out.println("\n=== Login ===");
            System.out.println("DTO decifrado (User): " + mapper.writeValueAsString(clearData));

            System.out.println("\nUsuário de teste inicializado, cifrado no cadastro e validado no login!");
        };
    }
}
