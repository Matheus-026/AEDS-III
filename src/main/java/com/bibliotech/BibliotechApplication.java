package com.bibliotech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.bibliotech.compression.BackupManager;

@SpringBootApplication
public class BibliotechApplication {
    public static void main(String[] args) throws Exception{
        BackupManager.gerarBackupLZW();
        SpringApplication.run(BibliotechApplication.class, args);
    }
}