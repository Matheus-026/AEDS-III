package com.bibliotech;

import java.io.*;

/**
 * ReconstruirAutores.java
 *
 * Apaga data/autores.dat e os índices de autores,
 * depois recadastra todos os autores do JSON original.
 *
 * Como compilar e rodar (na raiz do projeto, onde fica a pasta data/):
 *
 *   Windows:
 *     javac -cp "target/classes;target/dependency/*" ReconstruirAutores.java
 *     java  -cp ".;target/classes;target/dependency/*" ReconstruirAutores
 *
 *   Linux/Mac:
 *     javac -cp "target/classes:target/dependency/*" ReconstruirAutores.java
 *     java  -cp ".:target/classes:target/dependency/*" ReconstruirAutores
 *
 * Pare o servidor Spring Boot antes de rodar!
 */
public class ReconstruirAutores {

    public static void main(String[] args) throws Exception {

        System.out.println("=== Reconstrução do arquivo de autores ===\n");

        // ── 1. Apaga arquivos corrompidos ─────────────────────────────────────
        String[] arquivosParaApagar = {
            "data/autores.dat",
            "data/diretorios/autores_dir.hash",
            "data/buckets/autores_bucket.hash"
        };

        for (String caminho : arquivosParaApagar) {
            java.io.File f = new java.io.File(caminho);
            if (f.exists()) {
                f.delete();
                System.out.println("[OK] Apagado: " + caminho);
            } else {
                System.out.println("[--] Não encontrado (ok): " + caminho);
            }
        }

        System.out.println();

        // ── 2. Instancia DAO ──────────────────────────────────────────────────
        com.bibliotech.dao.AutorDAO dao = new com.bibliotech.dao.AutorDAO();

        // ── 3. Dados dos autores (extraídos do JSON original) ─────────────────
        // { nome, telefone, biografia }
        Object[][] autores = {
            {
                "Joaquim Manuel de Machado",
                "(31) 98254-5698",
                "Joaquim Manuel de Machado, conhecido por \"A Moreninha\", nasceu em Itaboraí, Rio de Janeiro, em 24 de junho de 1820. Formou-se em Medicina em 1844 e, em seguida, se destacou como romancista, poeta e dramaturgo. Ele foi professor de História e Geografia no Colégio Pedro II e preceptor dos netos do Imperador Pedro II. Machado também foi um dos fundadores do Instituto Histórico e Geográfico Brasileiro e uma figura importante na política brasileira, atuando no Partido Liberal. Sua obra \"A Moreninha\" é considerada uma das mais influentes do Romantismo brasileiro, refletindo os costumes da burguesia carioca do século XIX."
            },
            {
                "Machado de Assis",
                "(31) 91234-5678",
                "Biografia: Machado de Assis foi um dos maiores escritores da literatura brasileira, fundador da Academia Brasileira de Letras. Sua obra é marcada por ironia, crítica social e profundidade psicológica."
            },
            {
                "Rafael Monteiro",
                "(21) 93456-7890",
                "Rafael Monteiro é um escritor brasileiro contemporâneo, nascido em 1985, conhecido por suas obras que misturam drama e natureza. Formado em Letras, começou sua carreira escrevendo contos e se destacou com romances que exploram conflitos humanos."
            },
            {
                "Camila Duarte",
                "(11) 98765-4321",
                "Camila Duarte nasceu em 1990 e é uma autora de ficção científica. Suas histórias abordam viagens no tempo e dilemas éticos. Ganhou notoriedade entre jovens leitores por sua escrita envolvente."
            },
            {
                "Eduardo Lima",
                "(21) 97654-3210",
                "Eduardo Lima é um escritor brasileiro conhecido por romances policiais. Nascido em 1978, trabalhou como jornalista antes de se dedicar à literatura, trazendo realismo às suas narrativas."
            },
            {
                "Fernanda Alves",
                "(41) 96543-2109",
                "Fernanda Alves, nascida em 1995, é uma jovem escritora que se destaca no gênero romance contemporâneo. Suas obras abordam relações humanas e autoconhecimento."
            },
            {
                "Lucas Martins",
                "(51) 95432-1098",
                "Lucas Martins é um autor brasileiro e desenvolvedor de software. Nascido em 1988, combina tecnologia e narrativa em seus livros, explorando temas como inteligência artificial e segurança digital."
            },
            {
                "Juliana Rocha",
                "(61) 94321-0987",
                "Juliana Rocha nasceu em 1983 e é conhecida por suas histórias emocionantes com forte protagonismo feminino. Formada em Psicologia, suas obras exploram sentimentos profundos."
            },
            {
                "André Carvalho",
                "(71) 93210-9876",
                "André Carvalho é um escritor de thrillers psicológicos. Nascido em 1975, suas obras são marcadas por reviravoltas e personagens complexos."
            },
            {
                "Beatriz Fernandes",
                "(81) 92109-8765",
                "Beatriz Fernandes é uma autora de fantasia urbana, nascida em 1992. Suas histórias combinam elementos sobrenaturais com o cotidiano moderno."
            },
            {
                "Carolina Maria de Jesus",
                "(42) 98653-1546",
                "Carolina nasceu em 14 de março de 1914, em Sacramento, Minas Gerais, filha de pais negros e neta de ex-escravizados, em uma família muito pobre. Teve apenas dois anos de estudo formal, mas desde cedo desenvolveu paixão pela leitura e escrita, incentivada por pessoas próximas à sua família."
            },
            {
                "Mauricio de Sousa",
                "(56) 98653-4519",
                "Maurício Araújo de Sousa nasceu em Santa Isabel, São Paulo, em uma família de artistas. Desde pequeno, demonstrou interesse por desenho e começou a trabalhar como repórter policial, onde criou seu primeiro personagem, o cão Bidu, que foi publicado em 1959. Sua paixão por quadrinhos o levou a desenvolver personagens icônicos como Mônica, Cebolinha, Cascão e Magali, que se tornaram símbolos da cultura popular brasileira."
            },
            {
                "Carlos Drummond de Andrade",
                "(45) 95642-4562",
                "Carlos Drummond de Andrade nasceu em Itabira, Minas Gerais, no dia 31 de outubro de 1902. Ele era o nono filho de uma família de fazendeiros. Desde jovem, demonstrou interesse pela literatura, influenciado por autores como Machado de Assis. Em 1916, começou seus estudos em Belo Horizonte e, em 1921, começou a publicar seus primeiros textos no Diário de Minas. Formou-se em Farmácia pela Escola de Odontologia e Farmácia de Belo Horizonte, mas nunca exerceu a profissão."
            },
            {
                "Machado de Assis - Ed. Atualizada",
                "",
                ""
            },
        };

        // ── 4. Cadastra cada autor ────────────────────────────────────────────
        int erros = 0;
        for (Object[] a : autores) {
            try {
                com.bibliotech.model.Autor autor = new com.bibliotech.model.Autor(
                    (String) a[0],  // nome
                    (String) a[1],  // telefone
                    (String) a[2]   // biografia
                );

                int id = dao.create(autor);
                System.out.printf("[OK] id=%-3d  %s%n", id, a[0]);

            } catch (Exception ex) {
                erros++;
                System.err.println("[ERRO] " + a[0] + " → " + ex.getMessage());
            }
        }

        dao.fechar();

        System.out.println();
        System.out.println("=== Concluído — " + autores.length + " autores processados, " + erros + " erro(s) ===");

        if (erros == 0) {
            System.out.println("Arquivo data/autores.dat reconstruído com sucesso!");
            System.out.println("Pode reiniciar o servidor Spring normalmente.");
        } else {
            System.out.println("Verifique os erros acima antes de reiniciar o servidor.");
        }
    }
}