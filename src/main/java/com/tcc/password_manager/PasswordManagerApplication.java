package com.tcc.password_manager;

import com.tcc.password_manager.cli.commands.MenuCommands;
import com.tcc.password_manager.dto.FragmentedData;
import com.tcc.password_manager.service.FragmentationService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PasswordManagerApplication {
    
    private static MenuCommands menu = new MenuCommands();
    
    public static void main(String[] args) {
        SpringApplication.run(PasswordManagerApplication.class, args);
    }
   

}
