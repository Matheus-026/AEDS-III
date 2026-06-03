package com.bibliotech.compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BackupManager {

    public static void gerarBackupLZW() throws IOException {

        String[] arquivos = {
                "data/livros.dat",
                "data/autores.dat",
                "data/usuarios.dat",
                "data/emprestimos.dat"
        };

        System.out.println("Verificando arquivos...");

        for (String caminho : arquivos) {

            File arquivo = new File(caminho);

            if (arquivo.exists()) {
                System.out.println(
                        caminho + " -> " +
                                arquivo.length() + " bytes");
            } else {
                System.out.println(
                        caminho + " -> NÃO ENCONTRADO");
            }
        }

        // Cria a pasta backup caso não exista
        File pastaBackup = new File("backup");

        if (!pastaBackup.exists()) {
            pastaBackup.mkdir();
        }

        // Junta todos os .dat em um único arquivo
        criarBackupCompleto();

        // Compacta o backup completo
        LZW.compactar(
                "backup/backup_completo.dat",
                "backup/backup_completo.lzw");

        Huffman.compactar(
                "backup/backup_completo.dat",
                "backup/backup_completo.huff");

        System.out.println("Backup único compactado criado com sucesso!");
    }

    private static void criarBackupCompleto() throws IOException {

        String[] arquivos = {
                "data/livros.dat",
                "data/autores.dat",
                "data/usuarios.dat",
                "data/emprestimos.dat"
        };

        FileOutputStream out = new FileOutputStream("backup/backup_completo.dat");

        for (String caminho : arquivos) {

            FileInputStream in = new FileInputStream(caminho);

            byte[] buffer = new byte[1024];
            int bytesLidos;

            while ((bytesLidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLidos);
            }

            in.close();
        }

        out.close();

        System.out.println(
                "Arquivo backup_completo.dat criado com sucesso!");
    }

    private static void copiarArquivo(
            String origem,
            String destino) throws IOException {

        FileInputStream in = new FileInputStream(origem);
        FileOutputStream out = new FileOutputStream(destino);

        byte[] buffer = new byte[1024];
        int bytesLidos;

        while ((bytesLidos = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesLidos);
        }

        in.close();
        out.close();
    }
}