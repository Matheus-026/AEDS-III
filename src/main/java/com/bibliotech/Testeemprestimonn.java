package com.bibliotech;

import com.bibliotech.dao.EmprestimoDAO;
import com.bibliotech.model.Emprestimo;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.List;

/**
 * Testa o EmprestimoDAO com foco no relacionamento N:N e chave composta.
 *
 * Fluxo:
 *  0. Reconstrói os quatro índices (PK + composta + usuario + livro)
 *  1. Inspeciona o emprestimos.dat existente
 *  2. Verifica navegação N:N nos dados existentes (ambos os lados)
 *  3. Testa validação da chave composta (impede par duplicado ativo)
 *  4. Testa buscarAtivoPorPar
 *  5. Testa registrarDevolucao por par (sem idEmprestimo)
 *  6. Testa que após devolução o mesmo par pode ser reinserido
 *  7. Testa delete e remoção dos índices
 *  8. Reconstrói e verifica consistência final
 */
public class Testeemprestimonn {

    // ── ajuste para IDs reais nos seus .dat ───────────────────────────────────
    private static final int ID_USUARIO_A = 1;
    private static final int ID_USUARIO_B = 2;
    private static final int ID_LIVRO_X   = 1;
    private static final int ID_LIVRO_Y   = 2;
    private static final int ID_LIVRO_Z   = 3;
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("   TESTE N:N — EMPRESTIMO (chave composta)");
        System.out.println("=================================================\n");

        try {
            EmprestimoDAO dao = new EmprestimoDAO();

            // =================================================================
            // 0. RECONSTRUÇÃO DOS QUATRO ÍNDICES
            // =================================================================
            secao("0. RECONSTRUINDO ÍNDICES (PK + composta + usuario + livro)");
            dao.reconstruirIndices();
            ok("Quatro índices reconstruídos");

            // =================================================================
            // 1. INSPECIONAR O .DAT EXISTENTE
            // =================================================================
            secao("1. ESTADO ATUAL EM DISCO (emprestimos.dat)");
            inspecionarDat();

            // =================================================================
            // 2. NAVEGAÇÃO N:N NOS DADOS EXISTENTES
            // =================================================================
            secao("2. NAVEGAÇÃO N:N — DADOS EXISTENTES");

            info("── Usuário " + ID_USUARIO_A + " → livros emprestados:");
            List<Emprestimo> listaUA = dao.buscarPorUsuario(ID_USUARIO_A);
            info("   " + listaUA.size() + " empréstimo(s)");
            listaUA.forEach(e -> info("   -> id=" + e.getId()
                + " | livro=" + e.getIdLivro()
                + " | status=" + e.getStatus()
                + " | devolução=" + e.getDataDevolucao()));

            info("── Livro " + ID_LIVRO_X + " → usuários que pegaram emprestado:");
            List<Emprestimo> listaLX = dao.buscarPorLivro(ID_LIVRO_X);
            info("   " + listaLX.size() + " empréstimo(s)");
            listaLX.forEach(e -> info("   -> id=" + e.getId()
                + " | usuario=" + e.getIdUsuario()
                + " | status=" + e.getStatus()));

            assertVazio(dao.buscarPorUsuario(99999),
                "buscarPorUsuario(99999) deve retornar vazio");
            assertVazio(dao.buscarPorLivro(99999),
                "buscarPorLivro(99999) deve retornar vazio");

            // =================================================================
            // 3. CHAVE COMPOSTA — impede par duplicado ativo
            // =================================================================
            secao("3. VALIDAÇÃO DA CHAVE COMPOSTA (unicidade do par)");

            // insere empréstimo novo para um par limpo
            Emprestimo e1 = emprestimo(ID_USUARIO_B, ID_LIVRO_Z,
                LocalDate.now(), LocalDate.now().plusDays(14));
            int id1 = dao.create(e1);
            ok("Criado id=" + id1 + " | par=(" + ID_USUARIO_B + "," + ID_LIVRO_Z + ")");

            // tenta inserir o mesmo par ativo — deve lançar exceção
            boolean bloqueou = false;
            try {
                Emprestimo duplicado = emprestimo(ID_USUARIO_B, ID_LIVRO_Z,
                    LocalDate.now(), LocalDate.now().plusDays(7));
                dao.create(duplicado);
            } catch (IllegalStateException ex) {
                bloqueou = true;
                ok("Par duplicado ativo bloqueado corretamente: " + ex.getMessage());
            }
            assertTrue(bloqueou,
                "Inserção de par (" + ID_USUARIO_B + "," + ID_LIVRO_Z + ") duplicado ativo deve ser bloqueada");

            // =================================================================
            // 4. BUSCAR POR PAR (buscarAtivoPorPar)
            // =================================================================
            secao("4. BUSCA DIRETA POR PAR (chave composta)");

            Emprestimo encontrado = dao.buscarAtivoPorPar(ID_USUARIO_B, ID_LIVRO_Z);
            assertNaoNulo(encontrado,
                "buscarAtivoPorPar(" + ID_USUARIO_B + "," + ID_LIVRO_Z + ") deve encontrar");
            info("Encontrado: id=" + encontrado.getId()
                + " | status=" + encontrado.getStatus());

            Emprestimo naoEncontrado = dao.buscarAtivoPorPar(99999, 99999);
            assertNulo(naoEncontrado,
                "buscarAtivoPorPar(99999,99999) deve retornar null");

            // =================================================================
            // 5. DEVOLUÇÃO POR PAR (registrarDevolucao)
            // =================================================================
            secao("5. DEVOLUÇÃO POR PAR (sem precisar do idEmprestimo)");

            Emprestimo devolvido = dao.registrarDevolucao(ID_USUARIO_B, ID_LIVRO_Z);
            assertNaoNulo(devolvido, "registrarDevolucao deve retornar o empréstimo");
            assertEqual(devolvido.getStatus(), "Devolvido", "status após devolução");
            info("Devolvido: id=" + devolvido.getId() + " | status=" + devolvido.getStatus());

            // confirma que não aparece mais como ativo no índice de chave composta
            Emprestimo aposDevol = dao.buscarAtivoPorPar(ID_USUARIO_B, ID_LIVRO_Z);
            assertNulo(aposDevol,
                "buscarAtivoPorPar após devolução deve retornar null");

            // devolução de par inexistente deve retornar null
            Emprestimo devolInexistente = dao.registrarDevolucao(99999, 99999);
            assertNulo(devolInexistente,
                "registrarDevolucao de par inexistente deve retornar null");

            // =================================================================
            // 6. REUTILIZAÇÃO DO PAR APÓS DEVOLUÇÃO
            // =================================================================
            secao("6. REINSERÇÃO DO MESMO PAR APÓS DEVOLUÇÃO");

            Emprestimo novoEmprestimo = emprestimo(ID_USUARIO_B, ID_LIVRO_Z,
                LocalDate.now(), LocalDate.now().plusDays(10));
            int id2 = dao.create(novoEmprestimo);
            ok("Par (" + ID_USUARIO_B + "," + ID_LIVRO_Z + ") reinserido com id=" + id2 + " após devolução");

            Emprestimo novoAtivo = dao.buscarAtivoPorPar(ID_USUARIO_B, ID_LIVRO_Z);
            assertNaoNulo(novoAtivo, "novo empréstimo ativo para o par deve ser encontrado");
            assertEqual(novoAtivo.getStatus(), "Em aberto", "status do novo empréstimo");

            // =================================================================
            // 7. DELETE — verifica remoção dos quatro índices
            // =================================================================
            secao("7. DELETE — verificando remoção de todos os índices");

            int qtdUA_antes = dao.buscarPorUsuario(ID_USUARIO_B).size();
            int qtdLZ_antes = dao.buscarPorLivro(ID_LIVRO_Z).size();

            assertTrue(dao.delete(id2), "delete(id=" + id2 + ") deve retornar true");
            assertNulo(dao.read(id2),   "read(id=" + id2 + ") após delete deve ser null");

            // par não deve mais estar ativo
            assertNulo(dao.buscarAtivoPorPar(ID_USUARIO_B, ID_LIVRO_Z),
                "buscarAtivoPorPar após delete deve retornar null");

            // índices N:N devem refletir a remoção
            assertEqual(dao.buscarPorUsuario(ID_USUARIO_B).size(), qtdUA_antes - 1,
                "usuário B deve ter -1 após delete");
            assertEqual(dao.buscarPorLivro(ID_LIVRO_Z).size(), qtdLZ_antes - 1,
                "livro Z deve ter -1 após delete");

            assertFalse(dao.delete(99999), "delete(id=99999) deve retornar false");
            assertFalse(dao.delete(id2),   "segundo delete deve retornar false");

            // =================================================================
            // 8. RECONSTRUÇÃO FINAL
            // =================================================================
            secao("8. RECONSTRUÇÃO FINAL (simula reinício da aplicação)");

            dao.reconstruirIndices();
            ok("Índices reconstruídos");

            // verifica que os dados persistiram corretamente
            assertNaoNulo(dao.read(id1),
                "id=" + id1 + " (devolvido) deve ser encontrado após reconstrução");
            assertEqual(dao.read(id1).getStatus(), "Devolvido",
                "status do id=" + id1 + " após reconstrução");

            assertNulo(dao.read(id2),
                "id=" + id2 + " (deletado) não deve aparecer após reconstrução");

            assertNulo(dao.buscarAtivoPorPar(ID_USUARIO_B, ID_LIVRO_Z),
                "par (" + ID_USUARIO_B + "," + ID_LIVRO_Z + ") não deve estar ativo após reconstrução");

            // =================================================================
            // 9. ESTADO FINAL — AMBOS OS LADOS DO N:N
            // =================================================================
            secao("9. ESTADO FINAL — NAVEGAÇÃO N:N");

            info("Usuário " + ID_USUARIO_A + " → " + dao.buscarPorUsuario(ID_USUARIO_A).size() + " empréstimo(s):");
            dao.buscarPorUsuario(ID_USUARIO_A).forEach(e ->
                info("  -> id=" + e.getId() + " | livro=" + e.getIdLivro() + " | " + e.getStatus()));

            info("Usuário " + ID_USUARIO_B + " → " + dao.buscarPorUsuario(ID_USUARIO_B).size() + " empréstimo(s):");
            dao.buscarPorUsuario(ID_USUARIO_B).forEach(e ->
                info("  -> id=" + e.getId() + " | livro=" + e.getIdLivro() + " | " + e.getStatus()));

            info("Livro " + ID_LIVRO_X + " → " + dao.buscarPorLivro(ID_LIVRO_X).size() + " empréstimo(s):");
            dao.buscarPorLivro(ID_LIVRO_X).forEach(e ->
                info("  -> id=" + e.getId() + " | usuario=" + e.getIdUsuario() + " | " + e.getStatus()));

            info("Livro " + ID_LIVRO_Z + " → " + dao.buscarPorLivro(ID_LIVRO_Z).size() + " empréstimo(s):");
            dao.buscarPorLivro(ID_LIVRO_Z).forEach(e ->
                info("  -> id=" + e.getId() + " | usuario=" + e.getIdUsuario() + " | " + e.getStatus()));

            dao.fechar();

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
    private static void inspecionarDat() throws Exception {
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
                        + " | par=(" + e.getIdUsuario() + "," + e.getIdLivro() + ")"
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
                                          LocalDate dataEmp, LocalDate dataDev) {
        Emprestimo e = new Emprestimo();
        e.setIdUsuario(idUsuario);
        e.setIdLivro(idLivro);
        e.setDataEmprestimo(dataEmp);
        e.setDataDevolucao(dataDev);
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
        if (!valor) throw new AssertionError(descricao + " → esperava true");
        ok(descricao + " ✔");
    }

    private static void assertFalse(boolean valor, String descricao) {
        if (valor) throw new AssertionError(descricao + " → esperava false");
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