package com.bibliotech;

import com.bibliotech.dao.AutorDAO;
import com.bibliotech.model.Autor;

import java.io.RandomAccessFile;

public class TesteHashAutor {

    public static void main(String[] args) throws Exception {

        System.out.println("=================================");
        System.out.println(" TESTE HASH EXTENSÍVEL - AUTORES ");
        System.out.println("=================================");

        // 🔥 PASSO 0 - LIMPAR HASH (SEM APAGAR .dat)
        limparArquivosHash();

        AutorDAO dao = new AutorDAO();

        // 🔥 PASSO 1 - reconstruir hash
        dao.reconstruirHash();

        // 🔥 PASSO 2 - pegar último ID direto do arquivo
        RandomAccessFile arq = new RandomAccessFile("data/autores.dat", "r");
        arq.seek(0);
        int ultimoID = arq.readInt();

        int ultimoValido = -1;

        arq.seek(8);

        while (arq.getFilePointer() < arq.length()) {

            long pos = arq.getFilePointer();

            boolean ativo = arq.readBoolean();
            int tam = arq.readInt();

            byte[] dados = new byte[tam];
            arq.readFully(dados);

            if (ativo) {
                Autor a = new Autor();
                a.fromByteArray(dados);
                ultimoValido = a.getId();
            }
        }

        arq.close();

        System.out.println("\nÚltimo ID válido: " + ultimoValido);

        // 🔥 PASSO 3 - testar leitura do último autor (via HASH)
        Autor ultimo = dao.read(ultimoValido);

        if (ultimo != null) {
            System.out.println("✔ Último autor encontrado via HASH:");
            System.out.println("Nome: " + ultimo.getNome());
        } else {
            System.out.println("❌ ERRO: não encontrou o último autor");
        }

        // 🔥 PASSO 4 - testar alguns anteriores
        System.out.println("\nTestando registros anteriores:");

        for (int i = 1; i <= Math.min(5, ultimoID); i++) {
            Autor a = dao.read(i);

            if (a != null) {
                System.out.println("ID " + i + ": " + a.getNome());
            } else {
                System.out.println("ID " + i + ": NÃO ENCONTRADO");
            }
        }

        System.out.println("\n=================================");
        System.out.println(" TESTE FINALIZADO ");
        System.out.println("=================================");
    }

    // 🔥 MÉTODO NOVO (ESSENCIAL)
    private static void limparArquivosHash() throws Exception {

        RandomAccessFile dir = new RandomAccessFile("data/diretorios/autores_dir.hash", "rw");
        dir.setLength(0);
        dir.close();

        RandomAccessFile bucket = new RandomAccessFile("data/buckets/autores_bucket.hash", "rw");
        bucket.setLength(0);
        bucket.close();

        System.out.println("✔ Hash limpa (sem apagar .dat)");
    }
}