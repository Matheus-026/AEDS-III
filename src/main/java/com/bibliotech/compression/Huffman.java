package com.bibliotech.compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.PriorityQueue;

public class Huffman {

    public static void compactar(
            String arquivoEntrada,
            String arquivoSaida)
            throws IOException {

        System.out.println("Compactação Huffman iniciada...");

        byte[] dados =
                Files.readAllBytes(
                        Paths.get(arquivoEntrada));

        // Tabela de frequências
        int[] frequencias = new int[256];

        for (byte b : dados) {
            frequencias[b & 0xFF]++;
        }

        System.out.println("\nFrequência dos bytes encontrados:");

        for (int i = 0; i < 256; i++) {

            if (frequencias[i] > 0) {

                System.out.println(
                        "Byte "
                                + i
                                + " -> "
                                + frequencias[i]
                                + " ocorrências");
            }
        }

        // Fila de prioridade
        PriorityQueue<HuffmanNode> fila =
                new PriorityQueue<>(
                        (a, b) ->
                                Integer.compare(
                                        a.frequencia,
                                        b.frequencia));

        // Cria os nós folha
        for (int i = 0; i < 256; i++) {

            if (frequencias[i] > 0) {

                fila.add(
                        new HuffmanNode(
                                (byte) i,
                                frequencias[i]));
            }
        }

        // Monta a árvore
        while (fila.size() > 1) {

            HuffmanNode esquerda =
                    fila.poll();

            HuffmanNode direita =
                    fila.poll();

            HuffmanNode pai =
                    new HuffmanNode(
                            esquerda.frequencia
                                    + direita.frequencia,
                            esquerda,
                            direita);

            fila.add(pai);
        }

        HuffmanNode raiz = fila.poll();

        System.out.println();
        System.out.println(
                "Árvore de Huffman criada com sucesso!");

        System.out.println(
                "Frequência total: "
                        + raiz.frequencia);
    }
}