package com.tcc.password_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.password_manager.crypto.HashService;
import com.tcc.password_manager.crypto.KeyGeneratorService;
import com.tcc.password_manager.dto.AppUserClearData;
import com.tcc.password_manager.dto.PasswordReferenceClearData;
import com.tcc.password_manager.model.AppUser;
import com.tcc.password_manager.model.PasswordReference;
import com.tcc.password_manager.service.AppUserService;
import com.tcc.password_manager.service.PasswordReferenceService;
import java.util.Base64;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(AppUserService appUserService,
            PasswordReferenceService passwordReferenceService) {

        return args -> {
            // Serviços utilitários
            KeyGeneratorService keyGen = new KeyGeneratorService();
            HashService hashService = new HashService();
            ObjectMapper mapper = new ObjectMapper();

            // === Criando usuário 1 ===
            byte[] umkUser1 = keyGen.generateKey(); // chave AES-256 do user1
            String umkHash = hashService.hashToBase64(umkUser1);

            AppUserClearData user1Dto = new AppUserClearData(
                    "mfa1",
                    Base64.getEncoder().encodeToString(umkUser1), // simulação do wrapped
                    umkHash
            );

            System.out.println("DTO em claro (User1): " + mapper.writeValueAsString(user1Dto));

            AppUser user1 = appUserService.save(user1Dto, umkUser1);

            System.out.println("Entity salva no banco (User1): " + user1.getEncryptedData());

            // Decifrando para validar
            AppUserClearData user1Decrypted
                    = appUserService.decryptToDto(user1.getEncryptedData(), umkUser1);
            System.out.println("DTO decifrado (User1): " + mapper.writeValueAsString(user1Decrypted));

            // === Criando senha vinculada ao user1 ===
            PasswordReferenceClearData ref1Dto = new PasswordReferenceClearData(
                    "Email", "bcRef1", "bcRef2", "frag1"
            );
            System.out.println("DTO em claro (PasswordRef1): " + mapper.writeValueAsString(ref1Dto));

            PasswordReference ref1 = passwordReferenceService.save(ref1Dto, user1, umkUser1);

            System.out.println("Entity salva no banco (PasswordRef1): " + ref1.getEncryptedData());

            PasswordReferenceClearData ref1Decrypted
                    = passwordReferenceService.decryptToDto(ref1.getEncryptedData(), umkUser1);
            System.out.println("DTO decifrado (PasswordRef1): " + mapper.writeValueAsString(ref1Decrypted));

            // === Criando outra senha vinculada ao user1 ===
            PasswordReferenceClearData ref2Dto = new PasswordReferenceClearData(
                    "Banco", "bcRef3", "bcRef4", "frag2"
            );
            System.out.println("DTO em claro (PasswordRef2): " + mapper.writeValueAsString(ref2Dto));

            PasswordReference ref2 = passwordReferenceService.save(ref2Dto, user1, umkUser1);

            System.out.println("Entity salva no banco (PasswordRef2): " + ref2.getEncryptedData());

            PasswordReferenceClearData ref2Decrypted
                    = passwordReferenceService.decryptToDto(ref2.getEncryptedData(), umkUser1);
            System.out.println("DTO decifrado (PasswordRef2): " + mapper.writeValueAsString(ref2Decrypted));

            System.out.println("Usuário e senhas de teste inicializados com dados cifrados!");
        };
    }
}
