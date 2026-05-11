package com.tienda.console;

import com.tienda.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ConsoleRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ConsoleRunner.class);

    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private BackupService backupService;

    @Override
    public void run(String... args) {
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            log.info("Consola de comandos iniciada. Comandos: stop, restore <archivo.enc>");

            while (true) {
                try {
                    if (!scanner.hasNextLine()) break;
                    String command = scanner.nextLine().trim();

                    if ("stop".equalsIgnoreCase(command)) {
                        log.info("Comando 'stop' recibido. Apagando aplicacion...");
                        context.close();
                        break;
                    } else if (command.startsWith("restore ")) {
                        String filename = command.substring("restore ".length()).trim();
                        log.info("Restaurando backup: {}", filename);
                        backupService.restoreBackup(filename);
                        log.info("Backup restaurado exitosamente.");
                    } else if (!command.isEmpty()) {
                        log.info("Comando desconocido: {}", command);
                        log.info("Comandos disponibles: stop, restore <archivo.enc>");
                    }
                } catch (Exception e) {
                    log.error("Error en consola: {}", e.getMessage());
                }
            }

            scanner.close();
        });

        consoleThread.setDaemon(true);
        consoleThread.setName("console-runner");
        consoleThread.start();
    }
}
