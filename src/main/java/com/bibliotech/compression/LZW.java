package com.bibliotech.compression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LZW {

    public static void compactar(
            String arquivoEntrada,
            String arquivoSaida) throws IOException {

        byte[] dados = Files.readAllBytes(Paths.get(arquivoEntrada));

        Map<String, Integer> dicionario = new HashMap<>();

        for (int i = 0; i < 256; i++) {
            dicionario.put("" + (char) i, i);
        }

        int proximoCodigo = 256;

        String atual = "";

        List<Integer> resultado = new ArrayList<>();

        for (byte b : dados) {

            char c = (char) (b & 0xFF);

            String combinado = atual + c;

            if (dicionario.containsKey(combinado)) {

                atual = combinado;

            } else {

                resultado.add(dicionario.get(atual));

                dicionario.put(combinado, proximoCodigo);

                proximoCodigo++;

                atual = "" + c;
            }
        }

        if (!atual.isEmpty()) {
            resultado.add(dicionario.get(atual));
        }

        DataOutputStream out = new DataOutputStream(
                new FileOutputStream(arquivoSaida));

        for (Integer codigo : resultado) {
            out.writeShort(codigo);
        }

        out.close();

        System.out.println("Compressão concluída!");
        System.out.println("Original: " + dados.length + " bytes");

        File arquivoCompactado = new File(arquivoSaida);

        System.out.println(
                "Compactado: "
                        + arquivoCompactado.length()
                        + " bytes");

        double taxa = (1.0 -
                ((double) arquivoCompactado.length()
                        / dados.length))
                * 100.0;

        System.out.printf(
                "Taxa de compressão: %.2f%%\n",
                taxa);
    }

    public static void descompactar(
            String arquivoCompactado,
            String arquivoSaida) throws IOException {

        DataInputStream in = new DataInputStream(
                new FileInputStream(arquivoCompactado));

        System.out.println("Lendo códigos:");

        try {
            while (true) {
                int codigo = in.readShort() & 0xFFFF;
                System.out.print(codigo + " ");
            }

        } catch (EOFException e) {
            System.out.println();
            System.out.println("Fim do arquivo.");
        }
        in.close();
    }
}