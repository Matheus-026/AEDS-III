package com.bibliotech;
import java.io.*;
import java.time.LocalDate;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;

import java.util.List;

/**
 * ReconstruirIndicesLivros.java
 *
 * NÃO apaga livros.dat
 * NÃO perde dados
 *
 * Apenas recria os índices hash dos livros
 * a partir do arquivo data/livros.dat já existente.
 *
 * Uso:
 * 1. Pare o servidor Spring
 * 2. Execute este arquivo
 * 3. Inicie o servidor novamente
 */
public class ReconstruirIndicesLivros {

    public static void main(String[] args) {

        try {

            System.out.println("\n=================================");
            System.out.println("RECONSTRUÇÃO DOS ÍNDICES DE LIVROS");
            System.out.println("=================================\n");

            // ─── 1. Apaga SOMENTE os hashes ───────────────────────────────
            String[] hashes = {

                "data/diretorios/livros_dir.hash",
                "data/buckets/livros_bucket.hash",

                "data/diretorios/autorlivros_dir.hash",
                "data/buckets/autorlivros_bucket.hash"
            };

            for (String caminho : hashes) {

                java.io.File f = new java.io.File(caminho);

                if (f.exists()) {

                    boolean ok = f.delete();

                    if (ok) {
                        System.out.println("[OK] Hash apagado: " + caminho);
                    } else {
                        System.out.println("[ERRO] Não foi possível apagar: " + caminho);
                    }

                } else {

                    System.out.println("[--] Hash não encontrado: " + caminho);

                }
            }

            System.out.println();

            // ─── 2. Abre DAO ──────────────────────────────────────────────
            LivroDAO dao = new LivroDAO();

            // ─── 3. Reconstrói índices ────────────────────────────────────
            dao.reconstruirIndices();

            System.out.println("\n[OK] Índices reconstruídos com sucesso!");

            // ─── 4. Teste rápido ──────────────────────────────────────────
            System.out.println("\n=== TESTE DE LEITURA ===\n");

            for (int i = 1; i <= 5; i++) {

                Livro livro = dao.read(i);

                if (livro == null) {

                    System.out.println("Livro ID " + i + " -> NÃO ENCONTRADO");

                } else {

                    System.out.println(
                        "Livro ID " + i +
                        " -> " +
                        livro.getTitulo()
                    );
                }
            }

            dao.fechar();

            System.out.println("\n=================================");
            System.out.println("FINALIZADO");
            System.out.println("=================================\n");

        } catch (Exception e) {

            System.out.println("\nERRO DURANTE RECONSTRUÇÃO:\n");
            e.printStackTrace();

        }
    }
}