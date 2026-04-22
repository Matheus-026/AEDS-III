package com.bibliotech;

import com.bibliotech.dao.EmprestimoDAO;
import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Emprestimo;
import com.bibliotech.model.Usuario;
import com.bibliotech.model.Livro;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.List;

/**
 * Testa o EmprestimoDAO cobrindo:
 *  - CRUD completo com índice primário (PK)
 *  - Relacionamento 1:N: usuário → empréstimos  (hashUsuario)
 *  - Relacionamento 1:N: livro   → empréstimos  (hashLivro)
 *  - Sincronização dos índices no update e delete
 *  - Atualização automática de status (Em aberto / Atrasado / Devolvido)
 *  - Persistência após reconstrução dos índices
 */
public class TesteEmprestimoDAO {

    // ── ajuste para IDs reais nos seus .dat ───────────────────────────────────
    private static final int ID_USUARIO_A = 1;
    private static final int ID_USUARIO_B = 2;
    private static final int ID_LIVRO_X   = 1;
    private static final int ID_LIVRO_Y   = 2;
    private static final int ID_LIVRO_Z   = 3;
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("   TESTE EMPRESTIMO DAO  (PK + 1:N usuário/livro)");
        System.out.println("=================================================\n");

        try {
            EmprestimoDAO dao     = new EmprestimoDAO();
            UsuarioDAO usuarioDAO = new UsuarioDAO();
            LivroDAO   livroDAO   = new LivroDAO();

            // =================================================================
            // 0. RECONSTRUÇÃO DOS TRÊS ÍNDICES
            // =================================================================
            secao("0. RECONSTRUINDO ÍNDICES (PK + 1:N usuário + 1:N livro)");
            dao.reconstruirIndices();
            ok("Três índices reconstruídos");

            // =================================================================
            // 1. INSPECIONAR O .DAT EXISTENTE
            // =================================================================
            secao("1. ESTADO ATUAL EM DISCO (emprestimos.dat)");
            inspecionarEmprestimos();

            // =================================================================
            // 2. BUSCAR POR USUÁRIO E LIVRO — dados já existentes
            // =================================================================
            secao("2. CONSULTA 1:N NOS DADOS EXISTENTES");

            for (int idU : new int[]{ID_USUARIO_A, ID_USUARIO_B}) {
                Usuario u = usuarioDAO.read(idU);
                String nome = u != null ? u.getNome() : "NÃO ENCONTRADO";
                List<Emprestimo> lista = dao.buscarPorUsuario(idU);
                info("Usuário id=" + idU + " (" + nome + ") → " + lista.size() + " empréstimo(s)");
                lista.forEach(e -> info("    -> id=" + e.getId()
                    + " | livro=" + e.getIdLivro()
                    + " | status=" + e.getStatus()
                    + " | devolução=" + e.getDataDevolucao()));
            }

            for (int idL : new int[]{ID_LIVRO_X, ID_LIVRO_Y}) {
                Livro l = livroDAO.read(idL);
                String titulo = l != null ? l.getTitulo() : "NÃO ENCONTRADO";
                List<Emprestimo> lista = dao.buscarPorLivro(idL);
                info("Livro id=" + idL + " (" + titulo + ") → " + lista.size() + " empréstimo(s)");
                lista.forEach(e -> info("    -> id=" + e.getId()
                    + " | usuário=" + e.getIdUsuario()
                    + " | status=" + e.getStatus()));
            }

            assertVazio(dao.buscarPorUsuario(99999), "buscarPorUsuario(99999) deve retornar vazio");
            assertVazio(dao.buscarPorLivro(99999),   "buscarPorLivro(99999) deve retornar vazio");

            // =================================================================
            // 3. CREATE — insere empréstimos e verifica índices 1:N
            // =================================================================
            secao("3. CREATE — inserindo empréstimos e verificando índices 1:N");

            int qtdUA_antes = dao.buscarPorUsuario(ID_USUARIO_A).size();
            int qtdUB_antes = dao.buscarPorUsuario(ID_USUARIO_B).size();
            int qtdLX_antes = dao.buscarPorLivro(ID_LIVRO_X).size();
            int qtdLY_antes = dao.buscarPorLivro(ID_LIVRO_Y).size();
            int qtdLZ_antes = dao.buscarPorLivro(ID_LIVRO_Z).size();

            // empréstimo ativo (devolução no futuro)
            Emprestimo e1 = emprestimo(ID_USUARIO_A, ID_LIVRO_X,
                LocalDate.now(), LocalDate.now().plusDays(14));

            // empréstimo atrasado (devolução no passado)
            Emprestimo e2 = emprestimo(ID_USUARIO_A, ID_LIVRO_Y,
                LocalDate.now().minusDays(20), LocalDate.now().minusDays(6));

            // empréstimo de outro usuário
            Emprestimo e3 = emprestimo(ID_USUARIO_B, ID_LIVRO_Z,
                LocalDate.now(), LocalDate.now().plusDays(7));

            int id1 = dao.create(e1);
            int id2 = dao.create(e2);
            int id3 = dao.create(e3);

            ok("Criado id=" + id1 + " | usuário=" + ID_USUARIO_A + " | livro=" + ID_LIVRO_X + " | Em aberto");
            ok("Criado id=" + id2 + " | usuário=" + ID_USUARIO_A + " | livro=" + ID_LIVRO_Y + " | Atrasado");
            ok("Criado id=" + id3 + " | usuário=" + ID_USUARIO_B + " | livro=" + ID_LIVRO_Z + " | Em aberto");

            // verifica índice 1:N usuário
            assertEqual(dao.buscarPorUsuario(ID_USUARIO_A).size(), qtdUA_antes + 2,
                "usuário A deve ter +2 empréstimos no índice");
            assertEqual(dao.buscarPorUsuario(ID_USUARIO_B).size(), qtdUB_antes + 1,
                "usuário B deve ter +1 empréstimo no índice");

            // verifica índice 1:N livro
            assertEqual(dao.buscarPorLivro(ID_LIVRO_X).size(), qtdLX_antes + 1,
                "livro X deve ter +1 empréstimo no índice");
            assertEqual(dao.buscarPorLivro(ID_LIVRO_Y).size(), qtdLY_antes + 1,
                "livro Y deve ter +1 empréstimo no índice");
            assertEqual(dao.buscarPorLivro(ID_LIVRO_Z).size(), qtdLZ_antes + 1,
                "livro Z deve ter +1 empréstimo no índice");

            // =================================================================
            // 4. READ por PK + verificação de status
            // =================================================================
            secao("4. READ por PK e STATUS AUTOMÁTICO");

            Emprestimo lido1 = dao.read(id1);
            assertNaoNulo(lido1, "read(id=" + id1 + ")");
            lido1.atualizarStatus();
            assertEqual(lido1.getStatus(), "Em aberto", "status do empréstimo ativo");
            info("id=" + id1 + " | status=" + lido1.getStatus() + " | devolução=" + lido1.getDataDevolucao());

            Emprestimo lido2 = dao.read(id2);
            assertNaoNulo(lido2, "read(id=" + id2 + ")");
            lido2.atualizarStatus();
            assertEqual(lido2.getStatus(), "Atrasado", "status do empréstimo atrasado");
            info("id=" + id2 + " | status=" + lido2.getStatus() + " | devolução=" + lido2.getDataDevolucao());

            assertNulo(dao.read(99999), "read(id=99999) deve retornar null");

            // =================================================================
            // 5. UPDATE — devolução do livro (status → Devolvido)
            // =================================================================
            secao("5. UPDATE — registrando devolução (status → Devolvido)");

            Emprestimo paraDevolver = dao.read(id1);
            paraDevolver.setStatus("Devolvido");
            paraDevolver.setDataDevolucao(LocalDate.now());

            assertTrue(dao.update(paraDevolver), "update(id=" + id1 + ") deve retornar true");

            Emprestimo aposUpdate = dao.read(id1);
            assertNaoNulo(aposUpdate, "read após update não deve ser null");
            aposUpdate.atualizarStatus(); // não deve alterar pois já é Devolvido
            assertEqual(aposUpdate.getStatus(), "Devolvido", "status após devolução");
            info("id=" + id1 + " | status=" + aposUpdate.getStatus());

            assertFalse(dao.update(emprestimo(99999, 1,
                LocalDate.now(), LocalDate.now().plusDays(1))),
                "update(id=99999) deve retornar false");

            // =================================================================
            // 6. UPDATE — muda usuário (sincronização do índice 1:N)
            // =================================================================
            secao("6. UPDATE MUDANDO USUÁRIO (sincronização do índice 1:N)");

            int qtdA_antes = dao.buscarPorUsuario(ID_USUARIO_A).size();
            int qtdB_antes = dao.buscarPorUsuario(ID_USUARIO_B).size();

            info("Antes: usuário A tem " + qtdA_antes + " empréstimo(s)");
            info("Antes: usuário B tem " + qtdB_antes + " empréstimo(s)");

            Emprestimo mudaUsuario = dao.read(id2);
            mudaUsuario.setIdUsuario(ID_USUARIO_B); // migra do usuário A para o B

            dao.update(mudaUsuario);

            int qtdA_depois = dao.buscarPorUsuario(ID_USUARIO_A).size();
            int qtdB_depois = dao.buscarPorUsuario(ID_USUARIO_B).size();

            info("Depois: usuário A tem " + qtdA_depois + " empréstimo(s)");
            info("Depois: usuário B tem " + qtdB_depois + " empréstimo(s)");

            assertEqual(qtdA_depois, qtdA_antes - 1, "usuário A deve ter -1 após mudança");
            assertEqual(qtdB_depois, qtdB_antes + 1, "usuário B deve ter +1 após mudança");

            boolean estaEmB = dao.buscarPorUsuario(ID_USUARIO_B)
                                 .stream().anyMatch(e -> e.getId() == id2);
            boolean estaEmA = dao.buscarPorUsuario(ID_USUARIO_A)
                                 .stream().anyMatch(e -> e.getId() == id2);

            assertTrue(estaEmB,  "id=" + id2 + " deve estar na lista do usuário B");
            assertFalse(estaEmA, "id=" + id2 + " não deve estar mais na lista do usuário A");

            // =================================================================
            // 7. UPDATE — muda livro (sincronização do índice 1:N)
            // =================================================================
            secao("7. UPDATE MUDANDO LIVRO (sincronização do índice 1:N)");

            int qtdLY_antesUp = dao.buscarPorLivro(ID_LIVRO_Y).size();
            int qtdLZ_antesUp = dao.buscarPorLivro(ID_LIVRO_Z).size();

            Emprestimo mudaLivro = dao.read(id2);
            mudaLivro.setIdLivro(ID_LIVRO_Z); // migra do livro Y para o Z

            dao.update(mudaLivro);

            assertEqual(dao.buscarPorLivro(ID_LIVRO_Y).size(), qtdLY_antesUp - 1,
                "livro Y deve ter -1 após mudança");
            assertEqual(dao.buscarPorLivro(ID_LIVRO_Z).size(), qtdLZ_antesUp + 1,
                "livro Z deve ter +1 após mudança");

            assertTrue(dao.buscarPorLivro(ID_LIVRO_Z).stream().anyMatch(e -> e.getId() == id2),
                "id=" + id2 + " deve estar na lista do livro Z");
            assertFalse(dao.buscarPorLivro(ID_LIVRO_Y).stream().anyMatch(e -> e.getId() == id2),
                "id=" + id2 + " não deve estar mais na lista do livro Y");

            // =================================================================
            // 8. DELETE — verifica remoção dos índices 1:N
            // =================================================================
            secao("8. DELETE — verificando remoção dos índices 1:N");

            int qtdB_antDel = dao.buscarPorUsuario(ID_USUARIO_B).size();
            int qtdZ_antDel = dao.buscarPorLivro(ID_LIVRO_Z).size();

            assertTrue(dao.delete(id3), "delete(id=" + id3 + ") deve retornar true");
            assertNulo(dao.read(id3),   "read(id=" + id3 + ") após delete deve ser null");

            assertEqual(dao.buscarPorUsuario(ID_USUARIO_B).size(), qtdB_antDel - 1,
                "usuário B deve ter -1 após delete");
            assertEqual(dao.buscarPorLivro(ID_LIVRO_Z).size(), qtdZ_antDel - 1,
                "livro Z deve ter -1 após delete");

            assertFalse(dao.delete(99999), "delete(id=99999) deve retornar false");
            assertFalse(dao.delete(id3),   "segundo delete(id=" + id3 + ") deve retornar false");

            // =================================================================
            // 9. RECONSTRUÇÃO FINAL — verifica consistência dos três índices
            // =================================================================
            secao("9. RECONSTRUÇÃO FINAL (simula reinício da aplicação)");

            dao.reconstruirIndices();
            ok("Índices reconstruídos");

            // PK deve funcionar
            assertNaoNulo(dao.read(id1), "read(id=" + id1 + ") após reconstrução");
            assertNulo(dao.read(id3),    "id=" + id3 + " deletado não deve reaparecer");

            // índices 1:N devem refletir estado final
            info("Usuário A → " + dao.buscarPorUsuario(ID_USUARIO_A).size() + " empréstimo(s)");
            dao.buscarPorUsuario(ID_USUARIO_A).forEach(e ->
                info("  -> id=" + e.getId() + " | livro=" + e.getIdLivro() + " | " + e.getStatus()));

            info("Usuário B → " + dao.buscarPorUsuario(ID_USUARIO_B).size() + " empréstimo(s)");
            dao.buscarPorUsuario(ID_USUARIO_B).forEach(e ->
                info("  -> id=" + e.getId() + " | livro=" + e.getIdLivro() + " | " + e.getStatus()));

            info("Livro Z → " + dao.buscarPorLivro(ID_LIVRO_Z).size() + " empréstimo(s)");
            dao.buscarPorLivro(ID_LIVRO_Z).forEach(e ->
                info("  -> id=" + e.getId() + " | usuário=" + e.getIdUsuario()));

            // id3 não deve aparecer em nenhum índice 1:N
            assertFalse(dao.buscarPorUsuario(ID_USUARIO_B).stream().anyMatch(e -> e.getId() == id3),
                "id=" + id3 + " não deve aparecer no índice do usuário B após reconstrução");

            // =================================================================
            // 10. LIST ALL — estado final
            // =================================================================
            secao("10. LIST ALL — estado final");
            List<Emprestimo> final_ = dao.listAll();
            info("Total de empréstimos ativos: " + final_.size());
            final_.forEach(e -> info("  id=" + e.getId()
                + " | usuário=" + e.getIdUsuario()
                + " | livro=" + e.getIdLivro()
                + " | status=" + e.getStatus()));

            dao.fechar();
            usuarioDAO.fechar();
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
    // INSPEÇÃO DO .DAT
    // =========================================================================
    private static void inspecionarEmprestimos() throws Exception {
        try (RandomAccessFile arq = new RandomAccessFile("data/emprestimos.dat", "r")) {
            arq.seek(0);
            int ultimoID = arq.readInt();
            int qtd      = arq.readInt();
            info("Cabeçalho → ultimoID=" + ultimoID + " | qtd=" + qtd);

            arq.seek(8);
            while (arq.getFilePointer() < arq.length()) {
                long    pos   = arq.getFilePointer();
                boolean ativo = arq.readBoolean();
                int     tam   = arq.readInt();
                byte[]  dados = new byte[tam];
                arq.readFully(dados);

                if (ativo) {
                    Emprestimo e = new Emprestimo();
                    e.fromByteArray(dados);
                    e.atualizarStatus();
                    info("pos=" + pos
                        + " | id=" + e.getId()
                        + " | usuário=" + e.getIdUsuario()
                        + " | livro=" + e.getIdLivro()
                        + " | status=" + e.getStatus()
                        + " | devolução=" + e.getDataDevolucao());
                } else {
                    info("pos=" + pos + " | [REMOVIDO]");
                }
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private static Emprestimo emprestimo(int idUsuario, int idLivro,
                                         LocalDate dataEmprestimo,
                                         LocalDate dataDevolucao) {
        Emprestimo e = new Emprestimo();
        e.setIdUsuario(idUsuario);
        e.setIdLivro(idLivro);
        e.setDataEmprestimo(dataEmprestimo);
        e.setDataDevolucao(dataDevolucao);
        e.setStatus("Em aberto");
        return e;
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
            throw new AssertionError(
                descricao + " → esperava " + esperado + ", recebeu " + real);
        ok(descricao + " → " + real + " ✔");
    }

    private static void assertEqual(String real, String esperado, String descricao) {
        if (!real.equals(esperado))
            throw new AssertionError(
                descricao + " → esperava '" + esperado + "', recebeu '" + real + "'");
        ok(descricao + " → '" + real + "' ✔");
    }
}