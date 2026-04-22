package com.bibliotech;

import com.bibliotech.dao.AutorDAO;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Autor;
import com.bibliotech.model.Livro;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * Testa especificamente o relacionamento 1:N entre Autor e Livro.
 *
 * Fluxo:
 *  0. Reconstrói AMBOS os índices (PK + 1:N) a partir dos .dat existentes
 *  1. Inspeciona os .dat para mostrar o estado atual em disco
 *  2. Verifica buscarPorAutor para autores já cadastrados
 *  3. Cria livros novos vinculados a autores existentes e verifica o índice
 *  4. Testa update mudando autor (sincronização do índice 1:N)
 *  5. Testa delete e verifica remoção do índice 1:N
 *  6. Reconstrói os índices e verifica consistência final
 */
public class TesteRelacionamento1N {

    // ── ajuste para IDs que existam no seu autores.dat ────────────────────────
    private static final int ID_AUTOR_A = 1;
    private static final int ID_AUTOR_B = 2;
    private static final int ID_AUTOR_C = 3;
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("     TESTE RELACIONAMENTO 1:N  AUTOR -> LIVROS");
        System.out.println("=================================================\n");

        try {
            AutorDAO autorDAO = new AutorDAO();
            LivroDAO livroDAO = new LivroDAO();

            // =================================================================
            // 0. RECONSTRUÇÃO DOS DOIS ÍNDICES
            // =================================================================
            secao("0. RECONSTRUINDO ÍNDICES (PK + 1:N)");

            autorDAO.reconstruirHash();
            ok("Índice de autores reconstruído");

            livroDAO.reconstruirIndices();
            ok("Índices de livros reconstruídos (PK + autorlivros)");

            // =================================================================
            // 1. INSPECIONAR .DAT EXISTENTES
            // =================================================================
            secao("1. ESTADO ATUAL EM DISCO");

            info("── autores.dat ──");
            inspecionarAutores();

            info("\n── livros.dat ──");
            inspecionarLivros();

            // =================================================================
            // 2. BUSCAR POR AUTOR — dados já existentes
            // =================================================================
            secao("2. BUSCAR POR AUTOR (dados existentes no disco)");

            for (int idAutor : new int[]{ID_AUTOR_A, ID_AUTOR_B, ID_AUTOR_C}) {

                Autor autor = autorDAO.read(idAutor);
                String nomeAutor = autor != null ? autor.getNome() : "NÃO ENCONTRADO";

                List<Livro> livros = livroDAO.buscarPorAutor(idAutor);
                info("Autor id=" + idAutor + " (" + nomeAutor + ") → " + livros.size() + " livro(s)");
                livros.forEach(l -> info("    -> id=" + l.getId() + " | " + l.getTitulo()));
            }

            // autor sem livros cadastrados
            List<Livro> semLivros = livroDAO.buscarPorAutor(99999);
            assertVazio(semLivros, "buscarPorAutor(99999) deve retornar lista vazia");

            // =================================================================
            // 3. CREATE — insere livros e verifica índice 1:N imediatamente
            // =================================================================
            secao("3. CREATE — vinculando livros a autores e verificando índice 1:N");

            int qtdAntesA = livroDAO.buscarPorAutor(ID_AUTOR_A).size();
            int qtdAntesB = livroDAO.buscarPorAutor(ID_AUTOR_B).size();

            Livro l1 = livro("Memórias Póstumas",     ID_AUTOR_A, "Realismo",    45.00f);
            Livro l2 = livro("Quincas Borba",          ID_AUTOR_A, "Realismo",    38.00f);
            Livro l3 = livro("A Hora da Estrela",      ID_AUTOR_B, "Modernismo",  32.50f);

            int id1 = livroDAO.create(l1);
            int id2 = livroDAO.create(l2);
            int id3 = livroDAO.create(l3);

            ok("Criado id=" + id1 + " | " + l1.getTitulo() + " → autor " + ID_AUTOR_A);
            ok("Criado id=" + id2 + " | " + l2.getTitulo() + " → autor " + ID_AUTOR_A);
            ok("Criado id=" + id3 + " | " + l3.getTitulo() + " → autor " + ID_AUTOR_B);

            // verifica que o índice 1:N foi atualizado
            List<Livro> livrosA = livroDAO.buscarPorAutor(ID_AUTOR_A);
            assertEqual(livrosA.size(), qtdAntesA + 2,
                "autor " + ID_AUTOR_A + " deve ter +" + 2 + " livros no índice 1:N");
            livrosA.forEach(l -> info("  Autor " + ID_AUTOR_A + " → id=" + l.getId() + " | " + l.getTitulo()));

            List<Livro> livrosB = livroDAO.buscarPorAutor(ID_AUTOR_B);
            assertEqual(livrosB.size(), qtdAntesB + 1,
                "autor " + ID_AUTOR_B + " deve ter +" + 1 + " livro no índice 1:N");
            livrosB.forEach(l -> info("  Autor " + ID_AUTOR_B + " → id=" + l.getId() + " | " + l.getTitulo()));

            // =================================================================
            // 4. UPDATE — muda autor (sincronização crítica do índice 1:N)
            // =================================================================
            secao("4. UPDATE MUDANDO AUTOR (sincronização do índice 1:N)");

            int qtdA_antes = livroDAO.buscarPorAutor(ID_AUTOR_A).size();
            int qtdC_antes = livroDAO.buscarPorAutor(ID_AUTOR_C).size();

            info("Antes: autor " + ID_AUTOR_A + " tem " + qtdA_antes + " livro(s)");
            info("Antes: autor " + ID_AUTOR_C + " tem " + qtdC_antes + " livro(s)");

            // muda l2 do autor A para o autor C
            Livro l2Atualizado = livroDAO.read(id2);
            l2Atualizado.setIdAutor(ID_AUTOR_C);
            l2Atualizado.setTitulo("Quincas Borba - Ed. Revisada");

            boolean upOk = livroDAO.update(l2Atualizado);
            assertTrue(upOk, "update(id=" + id2 + ") deve retornar true");

            int qtdA_depois = livroDAO.buscarPorAutor(ID_AUTOR_A).size();
            int qtdC_depois = livroDAO.buscarPorAutor(ID_AUTOR_C).size();

            info("Depois: autor " + ID_AUTOR_A + " tem " + qtdA_depois + " livro(s)");
            info("Depois: autor " + ID_AUTOR_C + " tem " + qtdC_depois + " livro(s)");

            assertEqual(qtdA_depois, qtdA_antes - 1,
                "autor " + ID_AUTOR_A + " deve ter -1 livro após mudança");
            assertEqual(qtdC_depois, qtdC_antes + 1,
                "autor " + ID_AUTOR_C + " deve ter +1 livro após mudança");

            // confirma que l2 está na lista do autor C e não mais no A
            boolean estaEmC = livroDAO.buscarPorAutor(ID_AUTOR_C)
                                      .stream().anyMatch(l -> l.getId() == id2);
            boolean estaEmA = livroDAO.buscarPorAutor(ID_AUTOR_A)
                                      .stream().anyMatch(l -> l.getId() == id2);

            assertTrue(estaEmC,  "id=" + id2 + " deve estar na lista do autor " + ID_AUTOR_C);
            assertFalse(estaEmA, "id=" + id2 + " não deve estar mais na lista do autor " + ID_AUTOR_A);

            livroDAO.buscarPorAutor(ID_AUTOR_A).forEach(l ->
                info("  Autor " + ID_AUTOR_A + " → id=" + l.getId() + " | " + l.getTitulo()));
            livroDAO.buscarPorAutor(ID_AUTOR_C).forEach(l ->
                info("  Autor " + ID_AUTOR_C + " → id=" + l.getId() + " | " + l.getTitulo()));

            // =================================================================
            // 5. DELETE — verifica remoção do índice 1:N
            // =================================================================
            secao("5. DELETE — verificando remoção do índice 1:N");

            int qtdB_antes = livroDAO.buscarPorAutor(ID_AUTOR_B).size();
            info("Antes do delete: autor " + ID_AUTOR_B + " tem " + qtdB_antes + " livro(s)");

            boolean delOk = livroDAO.delete(id3);
            assertTrue(delOk, "delete(id=" + id3 + ") deve retornar true");

            // PK deve retornar null
            assertNulo(livroDAO.read(id3), "read(id=" + id3 + ") após delete deve retornar null");

            // índice 1:N deve refletir a remoção
            int qtdB_depois = livroDAO.buscarPorAutor(ID_AUTOR_B).size();
            info("Depois do delete: autor " + ID_AUTOR_B + " tem " + qtdB_depois + " livro(s)");
            assertEqual(qtdB_depois, qtdB_antes - 1,
                "autor " + ID_AUTOR_B + " deve ter -1 livro após delete");

            // o livro deletado não deve aparecer na lista do autor
            boolean deletadoAindaNaLista = livroDAO.buscarPorAutor(ID_AUTOR_B)
                                                   .stream().anyMatch(l -> l.getId() == id3);
            assertFalse(deletadoAindaNaLista,
                "id=" + id3 + " não deve aparecer na lista do autor " + ID_AUTOR_B + " após delete");

            // =================================================================
            // 6. RECONSTRUÇÃO FINAL — verifica consistência do índice 1:N
            // =================================================================
            secao("6. RECONSTRUÇÃO FINAL (simula reinício da aplicação)");

            livroDAO.reconstruirIndices();
            ok("Índices reconstruídos");

            // após reconstrução, as listas devem ser idênticas às de antes
            List<Livro> finalA = livroDAO.buscarPorAutor(ID_AUTOR_A);
            List<Livro> finalB = livroDAO.buscarPorAutor(ID_AUTOR_B);
            List<Livro> finalC = livroDAO.buscarPorAutor(ID_AUTOR_C);

            info("Autor " + ID_AUTOR_A + " → " + finalA.size() + " livro(s) após reconstrução");
            finalA.forEach(l -> info("  -> id=" + l.getId() + " | " + l.getTitulo()));

            info("Autor " + ID_AUTOR_B + " → " + finalB.size() + " livro(s) após reconstrução");
            finalB.forEach(l -> info("  -> id=" + l.getId() + " | " + l.getTitulo()));

            info("Autor " + ID_AUTOR_C + " → " + finalC.size() + " livro(s) após reconstrução");
            finalC.forEach(l -> info("  -> id=" + l.getId() + " | " + l.getTitulo()));

            // livro deletado não deve reaparecer
            assertNulo(livroDAO.read(id3),
                "id=" + id3 + " não deve reaparecer após reconstrução");

            // livro movido de autor deve estar no autor certo
            boolean l2NoCerto = livroDAO.buscarPorAutor(ID_AUTOR_C)
                                        .stream().anyMatch(l -> l.getId() == id2);
            assertTrue(l2NoCerto,
                "id=" + id2 + " deve estar no autor " + ID_AUTOR_C + " após reconstrução");

            autorDAO.fechar();
            livroDAO.fechar();

            System.out.println("\n=================================================");
            System.out.println("  TODOS OS TESTES PASSARAM ✔");
            System.out.println("=================================================");

        } catch (AssertionError e) {
            System.err.println("\n[FALHOU] " + e.getMessage());
        } catch (IOException e) {
            System.err.println("\n[ERRO DE IO] " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // INSPEÇÃO DOS .DAT
    // =========================================================================

    private static void inspecionarAutores() throws Exception {
        try (RandomAccessFile arq = new RandomAccessFile("data/autores.dat", "r")) {
            arq.seek(0);
            int ultimoID = arq.readInt();
            int qtd      = arq.readInt();
            info("  Cabeçalho → ultimoID=" + ultimoID + " | qtd=" + qtd);
            arq.seek(8);
            while (arq.getFilePointer() < arq.length()) {
                long    pos   = arq.getFilePointer();
                boolean ativo = arq.readBoolean();
                int     tam   = arq.readInt();
                byte[]  dados = new byte[tam];
                arq.readFully(dados);
                if (ativo) {
                    com.bibliotech.model.Autor a = new com.bibliotech.model.Autor();
                    a.fromByteArray(dados);
                    info("  pos=" + pos + " | id=" + a.getId() + " | " + a.getNome());
                } else {
                    info("  pos=" + pos + " | [REMOVIDO]");
                }
            }
        }
    }

    private static void inspecionarLivros() throws Exception {
        try (RandomAccessFile arq = new RandomAccessFile("data/livros.dat", "r")) {
            arq.seek(0);
            int ultimoID = arq.readInt();
            int qtd      = arq.readInt();
            info("  Cabeçalho → ultimoID=" + ultimoID + " | qtd=" + qtd);
            arq.seek(8);
            while (arq.getFilePointer() < arq.length()) {
                long    pos   = arq.getFilePointer();
                boolean ativo = arq.readBoolean();
                int     tam   = arq.readInt();
                byte[]  dados = new byte[tam];
                arq.readFully(dados);
                if (ativo) {
                    Livro l = new Livro();
                    l.fromByteArray(dados);
                    info("  pos=" + pos + " | id=" + l.getId()
                         + " | idAutor=" + l.getIdAutor()
                         + " | " + l.getTitulo());
                } else {
                    info("  pos=" + pos + " | [REMOVIDO]");
                }
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private static Livro livro(String titulo, int idAutor, String genero, float preco) {
        Livro l = new Livro();
        l.setTitulo(titulo);
        l.setResumo("");
        l.setPreco(preco);
        l.setDataPublicacao(java.time.LocalDate.of(2024, 1, 1));
        l.setGeneros(new String[]{genero});
        l.setIdAutor(idAutor);
        return l;
    }

    private static void secao(String nome) {
        System.out.println("\n─── " + nome + " ───");
    }

    private static void ok(String msg) {
        System.out.println("  [OK] " + msg);
    }

    private static void info(String msg) {
        System.out.println("  [INFO] " + msg);
    }

    private static void assertNaoNulo(Object obj, String descricao) {
        if (obj == null)
            throw new AssertionError(descricao + " → esperava objeto, recebeu null");
        ok(descricao + " ✔");
    }

    private static void assertNulo(Object obj, String descricao) {
        if (obj != null)
            throw new AssertionError(descricao + " → esperava null, recebeu " + obj);
        ok(descricao + " ✔");
    }

    private static void assertVazio(List<?> lista, String descricao) {
        if (!lista.isEmpty())
            throw new AssertionError(descricao + " → esperava vazia, tamanho=" + lista.size());
        ok(descricao + " ✔");
    }

    private static void assertTrue(boolean valor, String descricao) {
        if (!valor)
            throw new AssertionError(descricao + " → esperava true");
        ok(descricao + " ✔");
    }

    private static void assertFalse(boolean valor, String descricao) {
        if (valor)
            throw new AssertionError(descricao + " → esperava false");
        ok(descricao + " ✔");
    }

    private static void assertEqual(int real, int esperado, String descricao) {
        if (real != esperado)
            throw new AssertionError(descricao + " → esperava " + esperado + ", recebeu " + real);
        ok(descricao + " → " + real + " ✔");
    }

    private static void assertEqual(String real, String esperado, String descricao) {
        if (!real.equals(esperado))
            throw new AssertionError(descricao + " → esperava '" + esperado + "', recebeu '" + real + "'");
        ok(descricao + " → '" + real + "' ✔");
    }
}