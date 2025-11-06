package com.tcc.password_manager;

import com.tcc.password_manager.cli.CliRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PasswordManagerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(PasswordManagerApplication.class, args);

        CliRunner cli = ctx.getBean(CliRunner.class);
        // Thread separada (web + CLI)
        new Thread(() -> {
            try {
                cli.start();
            } catch (Exception e) {
                System.out.println("Erro na CLI: " + e.getMessage());
            }
        }, "cli-loop-thread").start();

    }

}
