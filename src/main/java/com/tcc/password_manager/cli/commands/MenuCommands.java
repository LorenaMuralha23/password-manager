package com.tcc.password_manager.cli.commands;

import jakarta.annotation.PostConstruct;
import java.util.Scanner;
import org.springframework.shell.standard.ShellComponent;

/**
 *
 * @author Lolo
 */
@ShellComponent
public class MenuCommands {


    public MenuCommands() {   
    }

    //@PostConstruct
    public void showMenuOnStart() {
        showMenu();
    }

    private void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println(" 🔐 Password Manager CLI ");
            System.out.println("================================");
            System.out.println("1. Registrar usuário");
            System.out.println("2. Login");
            System.out.println("3. Salvar credencial");
            System.out.println("4. Listar credenciais");
            System.out.println("5. Sair");
            System.out.println("================================");
            System.out.print("Escolha uma opção: ");

            String opcao = scanner.nextLine();

            switch (opcao) {
                case "1":
                    System.out.print("Digite o username: ");
                    String username = scanner.nextLine();
                    System.out.print("Digite a senha mestre: ");
                    String senhaMestre = scanner.nextLine();
                    break;

                case "2":
                    System.out.print("Digite o username: ");
                    username = scanner.nextLine();
                    System.out.print("Digite a senha mestre: ");
                    senhaMestre = scanner.nextLine();
                    break;

                case "3":
                    System.out.print("Digite o site: ");
                    String site = scanner.nextLine();
                    System.out.print("Digite o usuário: ");
                    String usuario = scanner.nextLine();
                    System.out.print("Digite a senha: ");
                    String senha = scanner.nextLine();
                    break;

                case "4":
                    break;

                case "5":
                    System.out.println("👋 Encerrando aplicação...");
                    running = false;
                    break;

                default:
                    System.out.println("❌ Opção inválida, tente novamente.");
            }
            System.out.flush();
        }

        scanner.close();
        
        System.exit(0);
    }

}
