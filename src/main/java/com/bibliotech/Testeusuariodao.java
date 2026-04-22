package com.bibliotech;

import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Usuario;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class Testeusuariodao {

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("         TESTE USUARIODAO - HASH EXTENSÍVEL");
        System.out.println("=================================================\n");

        try {
            UsuarioDAO dao = new UsuarioDAO();

            // =================================================================
            // 0. RECONSTRUÇÃO DOS ÍNDICES
            // =================================================================
            secao("0. RECONSTRUINDO ÍNDICES A PARTIR DO USUARIOS.DAT EXISTENTE");
            dao.reconstruirHash();
            ok("Índice reconstruído");

            // =================================================================
            // 1. INSPECIONAR O .DAT EXISTENTE
            // =================================================================
            secao("1. ESTADO ATUAL EM DISCO (usuarios.dat)");

            int ultimoID    = 0;
            int ultimoValido = -1;

            try (RandomAccessFile arq = new RandomAccessFile("data/usuarios.dat", "r")) {
                arq.seek(0);
                ultimoID     = arq.readInt();
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
                        Usuario u = new Usuario();
                        u.fromByteArray(dados);
                        ultimoValido = u.getId();
                        info("pos=" + pos + " | id=" + u.getId()
                            + " | nome=" + u.getNome()
                            + " | email=" + u.getEmail()
                            + " | tipo=" + u.getTipo());
                    } else {
                        info("pos=" + pos + " | [REMOVIDO]");
                    }
                }
            }

            info("Último ID válido: " + ultimoValido);

            // =================================================================
            // 2. READ via hash — dados existentes
            // =================================================================
            secao("2. READ via HASH (dados existentes)");

            Usuario ultimo = dao.read(ultimoValido);
            assertNaoNulo(ultimo, "read(id=" + ultimoValido + ") — último usuário válido");
            info("Último usuário: id=" + ultimo.getId() + " | " + ultimo.getNome());

            info("\nVarredura completa (id 1 até " + ultimoID + "):");
            for (int i = 1; i <= ultimoID; i++) {
                Usuario u = dao.read(i);
                System.out.println("  " + i + " → " + (u != null ? "OK | " + u.getNome() : "NULL"));
            }

            assertNulo(dao.read(99999), "read(id=99999) deve retornar null");

            // =================================================================
            // 3. LIST ALL
            // =================================================================
            secao("3. LIST ALL");
            List<Usuario> todos = dao.listAll();
            info("Total de usuários ativos: " + todos.size());
            todos.forEach(u -> info("  id=" + u.getId() + " | " + u.getNome() + " | " + u.getTipo()));

            // =================================================================
            // 4. CREATE
            // =================================================================
            secao("4. CREATE — inserindo usuários de teste");

            Usuario u1 = usuario("Ana Souza",    "ana@teste.com",    "senha123", "COMUM");
            Usuario u2 = usuario("Bruno Lima",   "bruno@teste.com",  "senha456", "COMUM");
            Usuario u3 = usuario("Carla Admin",  "carla@teste.com",  "admin999", "ADMIN");

            int id1 = dao.create(u1);
            int id2 = dao.create(u2);
            int id3 = dao.create(u3);

            ok("Criado → id=" + id1 + " | " + u1.getNome());
            ok("Criado → id=" + id2 + " | " + u2.getNome());
            ok("Criado → id=" + id3 + " | " + u3.getNome());

            assertNaoNulo(dao.read(id1), "read(id=" + id1 + ") após create");
            assertNaoNulo(dao.read(id2), "read(id=" + id2 + ") após create");
            assertNaoNulo(dao.read(id3), "read(id=" + id3 + ") após create");

            // =================================================================
            // 5. LOGIN
            // =================================================================
            secao("5. LOGIN");

            // login válido — usa senha antes de criptografar (dao criptografa internamente)
            Usuario logado = dao.login("ana@teste.com", "senha123");
            assertNaoNulo(logado, "login válido (ana@teste.com / senha123)");
            info("Logado: " + logado.getNome());

            // login com senha errada
            Usuario loginErrado = dao.login("ana@teste.com", "senhaerrada");
            assertNulo(loginErrado, "login com senha errada deve retornar null");

            // login com email inexistente
            Usuario loginInexistente = dao.login("naoexiste@teste.com", "senha123");
            assertNulo(loginInexistente, "login com email inexistente deve retornar null");

            // =================================================================
            // 6. UPDATE
            // =================================================================
            secao("6. UPDATE");

            Usuario paraAtualizar = dao.read(id1);
            paraAtualizar.setNome("Ana Souza Atualizada");

            boolean upOk = dao.update(paraAtualizar);
            assertTrue(upOk, "update(id=" + id1 + ") deve retornar true");

            Usuario aposUpdate = dao.read(id1);
            assertNaoNulo(aposUpdate, "read após update não deve ser null");
            assertEqual(aposUpdate.getNome(), "Ana Souza Atualizada", "nome após update");
            info("Após update: " + aposUpdate.getNome());

            // update em ID inexistente
            Usuario fantasma = usuario("Fantasma", "f@f.com", "x", "COMUM");
            fantasma.setId(99999);
            assertFalse(dao.update(fantasma), "update(id=99999) deve retornar false");

            // =================================================================
            // 7. DELETE
            // =================================================================
            secao("7. DELETE");

            assertTrue(dao.delete(id2),  "delete(id=" + id2 + ") deve retornar true");
            assertNulo(dao.read(id2),     "read após delete deve retornar null");
            assertFalse(dao.delete(99999),"delete(id=99999) deve retornar false");
            assertFalse(dao.delete(id2),  "segundo delete(id=" + id2 + ") deve retornar false");

            // =================================================================
            // 8. RECONSTRUÇÃO APÓS OPERAÇÕES
            // =================================================================
            secao("8. RECONSTRUÇÃO APÓS OPERAÇÕES (simula reinício)");

            dao.reconstruirHash();
            ok("Hash reconstruído");

            assertNaoNulo(dao.read(id1), "read(id=" + id1 + ") após reconstrução");
            assertEqual(dao.read(id1).getNome(), "Ana Souza Atualizada",
                        "nome preservado após reconstrução");
            assertNulo(dao.read(id2), "id=" + id2 + " deletado não deve reaparecer");

            // =================================================================
            // 9. ESTADO FINAL
            // =================================================================
            secao("9. ESTADO FINAL — TODOS OS USUÁRIOS ATIVOS");
            List<Usuario> final_ = dao.listAll();
            info("Total de usuários ativos: " + final_.size());
            final_.forEach(u -> info("  id=" + u.getId() + " | " + u.getNome() + " | " + u.getTipo()));

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
    // HELPERS
    // =========================================================================

    private static Usuario usuario(String nome, String email, String senha, String tipo) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(senha);
        u.setTipo(tipo);
        return u;
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

    private static void assertEqual(String real, String esperado, String descricao) {
        if (!real.equals(esperado))
            throw new AssertionError(
                descricao + " → esperava '" + esperado + "', recebeu '" + real + "'");
        ok(descricao + " → '" + real + "' ✔");
    }
}