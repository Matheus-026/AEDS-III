package com.bibliotech.compression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Huffman {

        public static void compactar(
                        String arquivoEntrada,
                        String arquivoSaida)
                        throws IOException {

                System.out.println("Compactação Huffman iniciada...");

                byte[] dados = Files.readAllBytes(
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
                PriorityQueue<HuffmanNode> fila = new PriorityQueue<>(
                                (a, b) -> Integer.compare(
                                                a.frequencia,
                                                b.frequencia));

                for (int i = 0; i < 256; i++) {

                        if (frequencias[i] > 0) {

                                fila.add(
                                                new HuffmanNode(
                                                                (byte) i,
                                                                frequencias[i]));
                        }
                }

                // Montagem da árvore
                while (fila.size() > 1) {

                        HuffmanNode esquerda = fila.poll();

                        HuffmanNode direita = fila.poll();

                        HuffmanNode pai = new HuffmanNode(
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

                // Gera os códigos
                Map<Byte, String> codigos = new HashMap<>();

                gerarCodigos(
                                raiz,
                                "",
                                codigos);

                System.out.println();
                System.out.println("Códigos Huffman:");

                for (Map.Entry<Byte, String> entrada : codigos.entrySet()) {

                        int valor = entrada.getKey() & 0xFF;

                        System.out.println(
                                        "Byte "
                                                        + valor
                                                        + " -> "
                                                        + entrada.getValue());
                }

                // =====================================
                // Compactação propriamente dita
                // =====================================

                StringBuilder bitsCompactados = new StringBuilder();

                for (byte b : dados) {

                        bitsCompactados.append(
                                        codigos.get(b));
                }

                FileOutputStream out = new FileOutputStream(
                                arquivoSaida);

                for (int i = 0; i < bitsCompactados.length(); i += 8) {

                        String byteString;

                        if (i + 8 <= bitsCompactados.length()) {

                                byteString = bitsCompactados.substring(
                                                i,
                                                i + 8);

                        } else {

                                byteString = bitsCompactados.substring(i);

                                while (byteString.length() < 8) {
                                        byteString += "0";
                                }
                        }

                        int valor = Integer.parseInt(
                                        byteString,
                                        2);

                        out.write(valor);
                }

                out.close();

                File arquivoCompactado = new File(arquivoSaida);

                System.out.println();
                System.out.println(
                                "Arquivo Huffman criado!");

                System.out.println(
                                "Original: "
                                                + dados.length
                                                + " bytes");

                System.out.println(
                                "Compactado: "
                                                + arquivoCompactado.length()
                                                + " bytes");

                double taxa = (1.0 -
                                ((double) arquivoCompactado.length()
                                                / dados.length))
                                * 100.0;

                System.out.printf(
                                "Taxa Huffman: %.2f%%\n",
                                taxa);
        }

        private static void gerarCodigos(
                        HuffmanNode no,
                        String codigo,
                        Map<Byte, String> codigos) {

                if (no == null) {
                        return;
                }

                // Nó folha
                if (no.esquerda == null
                                && no.direita == null) {

                        codigos.put(
                                        no.dado,
                                        codigo);

                        return;
                }

                gerarCodigos(
                                no.esquerda,
                                codigo + "0",
                                codigos);

                gerarCodigos(
                                no.direita,
                                codigo + "1",
                                codigos);
        }
}