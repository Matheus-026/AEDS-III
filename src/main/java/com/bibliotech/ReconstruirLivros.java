package com.bibliotech;
import java.io.*;
import java.time.LocalDate;

/**
 * ReconstruirLivros.java
 *
 * Apaga data/livros.dat e os índices de livros,
 * depois recadastra todos os livros do JSON original.
 *
 * Como compilar e rodar (na raiz do projeto, onde fica a pasta data/):
 *
 *   javac -cp "target/classes;target/dependency/*" ReconstruirLivros.java   (Windows)
 *   javac -cp "target/classes:target/dependency/*" ReconstruirLivros.java   (Linux/Mac)
 *
 *   java  -cp ".;target/classes;target/dependency/*" ReconstruirLivros      (Windows)
 *   java  -cp ".:target/classes:target/dependency/*" ReconstruirLivros      (Linux/Mac)
 *
 * O script usa as classes do próprio projeto (Livro, LivroDAO, HashExtensivel).
 * Certifique-se de que o projeto já foi compilado (mvn compile ou Build no IDE).
 */
public class ReconstruirLivros {

    public static void main(String[] args) throws Exception {

        System.out.println("=== Reconstrução do arquivo de livros ===\n");

        // ── 1. Apaga arquivos corrompidos ─────────────────────────────────────
        String[] arquivosParaApagar = {
            "data/livros.dat",
            "data/diretorios/livros_dir.hash",
            "data/buckets/livros_bucket.hash",
            "data/diretorios/autorlivros_dir.hash",
            "data/buckets/autorlivros_bucket.hash"
        };

        for (String caminho : arquivosParaApagar) {
            File f = new File(caminho);
            if (f.exists()) {
                f.delete();
                System.out.println("[OK] Apagado: " + caminho);
            } else {
                System.out.println("[--] Não encontrado (ok): " + caminho);
            }
        }

        System.out.println();

        // ── 2. Instancia DAO — cria arquivos zerados + índices limpos ─────────
        com.bibliotech.dao.LivroDAO dao = new com.bibliotech.dao.LivroDAO();

        // ── 3. Dados dos livros (extraídos do JSON original) ──────────────────
        Object[][] livros = {
            // { titulo, resumo, preco, dataPublicacao, generos[], idAutor }
            { "Entre Dois Mundos",                              "", 16.90f,  "2016-04-16", new String[]{"Fantasia","Aventura"},               10 },
            { "A Moreninha",                                    "", 18.99f,  "1800-02-09", new String[]{"Poesia","Romance"},                   1  },
            { "O Silêncio do Mar",                              "", 25.99f,  "2000-08-05", new String[]{"Romance"},                            9  },
            { "Turma Da Mônica",                                "", 26.60f,  "2018-06-16", new String[]{"Humor"},                              12 },
            { "Dom Casmurro",                                   "", 29.90f,  "1950-02-19", new String[]{"Romance"},                            2  },
            { "Quincas Borba - Ed. Revisada",                   "", 38.00f,  "2024-01-01", new String[]{"Realismo"},                           3  },
            { "Memórias Póstumas",                              "", 45.00f,  "2024-01-01", new String[]{"Realismo"},                           1  },
            { "A Última Estrela",                               "", 45.90f,  "2017-02-15", new String[]{"Psicológico","Drama"},                8  },
            { "Além do Horizonte Azul",                         "", 48.00f,  "2022-05-15", new String[]{"Romance","Drama"},                    6  },
            { "Fragmentos do Tempo",                            "", 48.50f,  "2020-06-07", new String[]{"Ficção","Suspense","Drama"},           4  },
            { "Código Invisível",                               "", 52.90f,  "2019-02-12", new String[]{"Tecnologia","Suspense"},              7  },
            { "Quarto de Despejo: Diário de uma Favelada",      "", 56.90f,  "1900-05-11", new String[]{"Realidade","Aventura"},               11 },
            { "O Eco das Montanhas",                            "", 59.90f,  "2018-09-04", new String[]{"Drama","Romance","Aventura"},          3  },
            { "Sombras de um Passado",                          "", 60.00f,  "2015-04-04", new String[]{"Mistério","Suspense"},                5  },
            { "Memórias Póstumas de Brás Cubas",                "", 82.50f,  "1881-04-05", new String[]{"Romance","Sátira"},                   2  },
            { "Casa de Alvenaria - Volume 1: Osasco",           "", 109.90f, "2021-05-12", new String[]{"Biografia","Histórias Reais"},        11 },
        };

        // ── 4. Cadastra cada livro ────────────────────────────────────────────
        int erros = 0;
        for (Object[] l : livros) {
            try {
                com.bibliotech.model.Livro livro = new com.bibliotech.model.Livro(
                    (String)   l[0],                        // titulo
                    (String)   l[1],                        // resumo
                    (float)    l[2],                        // preco
                    LocalDate.parse((String) l[3]),         // dataPublicacao
                    (String[]) l[4],                        // generos
                    (int)      l[5]                         // idAutor
                );

                int id = dao.create(livro);
                System.out.printf("[OK] id=%-3d  %s%n", id, l[0]);

            } catch (Exception ex) {
                erros++;
                System.err.println("[ERRO] " + l[0] + " → " + ex.getMessage());
            }
        }

        dao.fechar();

        System.out.println();
        System.out.println("=== Concluído — " + livros.length + " livros processados, " + erros + " erro(s) ===");

        if (erros == 0) {
            System.out.println("Arquivo data/livros.dat reconstruído com sucesso!");
            System.out.println("Pode reiniciar o servidor Spring normalmente.");
        } else {
            System.out.println("Verifique os erros acima antes de reiniciar o servidor.");
        }
    }
}