package com.bibliotech.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoyerMoore {

    /**
     * Constrói a tabela de saltos para a heurística do Caractere Ruim (Bad Character).
     */
    private static Map<Character, Integer> construirBadCharacter(String padrao) {
        Map<Character, Integer> badChar = new HashMap<>();
        int m = padrao.length();

        // Guarda a posição da última ocorrência de cada caractere no padrão.
        // Se o caractere se repetir, o valor será atualizado para a posição mais à direita.
        for (int i = 0; i < m; i++) {
            badChar.put(padrao.charAt(i), i);
        }
        return badChar;
    }

    /**
     * Busca todas as ocorrências do padrão dentro do texto usando Boyer-Moore.
     */
    public static List<Integer> buscar(String texto, String padrao) {
        List<Integer> ocorrencias = new ArrayList<>();

        if (texto == null || padrao == null || padrao.isEmpty()) {
            return ocorrencias;
        }

        // Case-insensitive: espelhando a mesma lógica usada no KMP
        String t = texto.toLowerCase();
        String p = padrao.toLowerCase();

        int n = t.length();
        int m = p.length();

        if (m > n) return ocorrencias;

        Map<Character, Integer> badChar = construirBadCharacter(p);

        int s = 0; // 's' é o deslocamento (shift) do padrão em relação ao texto

        while (s <= (n - m)) {
            int j = m - 1;

            // Boyer-Moore compara da DIREITA para a ESQUERDA
            while (j >= 0 && p.charAt(j) == t.charAt(s + j)) {
                j--;
            }

            // Se j < 0, significa que todo o padrão deu match
            if (j < 0) {
                ocorrencias.add(s);

                // Prepara o deslocamento para procurar a próxima ocorrência
                int avanco = 1;
                if (s + m < n) {
                    char proximoChar = t.charAt(s + m);
                    avanco = m - badChar.getOrDefault(proximoChar, -1);
                }
                s += avanco;
            } else {
                // Mismatch! Ocorre o salto da Heurística do Caractere Ruim
                char caractereRuim = t.charAt(s + j);
                int ultimaOcorrenciaNoPadrao = badChar.getOrDefault(caractereRuim, -1);
                
                // Math.max garante que o salto será sempre para a frente (mínimo de 1)
                s += Math.max(1, j - ultimaOcorrenciaNoPadrao);
            }
        }

        return ocorrencias;
    }

    /**
     * Verifica rapidamente se o padrão existe no texto.
     */
    public static boolean contem(String texto, String padrao) {
        return !buscar(texto, padrao).isEmpty();
    }
}