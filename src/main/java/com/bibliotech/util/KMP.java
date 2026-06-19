package com.bibliotech.util;

import java.util.ArrayList;
import java.util.List;

public class KMP {

    private static int[] construirLPS(String padrao) {
        int m   = padrao.length();
        int[] lps = new int[m];

        lps[0] = 0; // o primeiro elemento é sempre 0

        int comprimento = 0; // comprimento do prefixo-sufixo anterior
        int i = 1;

        while (i < m) {
            if (padrao.charAt(i) == padrao.charAt(comprimento)) {
                comprimento++;
                lps[i] = comprimento;
                i++;
            } else {
                if (comprimento != 0) {
                    // tenta o prefixo-sufixo anterior sem avançar i
                    comprimento = lps[comprimento - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }

        return lps;
    }

    public static List<Integer> buscar(String texto, String padrao) {
        List<Integer> ocorrencias = new ArrayList<>();

        if (texto == null || padrao == null || padrao.isEmpty()) {
            return ocorrencias;
        }

        // case-insensitive
        String t = texto.toLowerCase();
        String p = padrao.toLowerCase();

        int n   = t.length();
        int m   = p.length();

        if (m > n) return ocorrencias;

        int[] lps = construirLPS(p);

        int i = 0; // índice no texto
        int j = 0; // índice no padrão

        while (i < n) {
            if (t.charAt(i) == p.charAt(j)) {
                i++;
                j++;
            }

            if (j == m) {
                // encontrou ocorrência na posição i - j
                ocorrencias.add(i - j);
                // usa lps para continuar buscando sem retroceder no texto
                j = lps[j - 1];
            } else if (i < n && t.charAt(i) != p.charAt(j)) {
                if (j != 0) {
                    // usa lps para pular comparações já realizadas
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }

        return ocorrencias;
    }

  
    public static boolean contem(String texto, String padrao) {
        return !buscar(texto, padrao).isEmpty();
    }
}