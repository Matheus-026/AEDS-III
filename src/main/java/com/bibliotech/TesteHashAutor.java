package com.bibliotech;

import com.bibliotech.dao.AutorDAO;
import com.bibliotech.model.Autor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class TesteHashAutor {

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("          TESTE AUTORDAO - HASH EXTENSÍVEL");
        System.out.println("=================================================\n");

        try {
            AutorDAO dao = new AutorDAO();

            // =================================================================
            // 0. RECONSTRUÇÃO DOS ÍNDICES
            //    Limpa os .hash e reconstrói a partir do autores.dat existente
            // =================================================================
            secao("0. RECONSTRUINDO ÍNDICES A PARTIR DO AUTORES.DAT EXISTENTE");
            dao.reconstruirHash();
            ok("Índices reconstruídos");

            // =================================================================
            // 1. INSPECIONAR O .DAT EXISTENTE
            //    Lê diretamente o arquivo para saber quais IDs existem
            // =================================================================
            secao("1. DADOS EXISTENTES NO AUTORES.DAT");

            int ultimoID    = 0;
            int ultimoValido = -1;

            try (RandomAccessFile arq = new RandomAccessFile("data/autores.dat", "r")) {

                arq.seek(0);
                ultimoID = arq.readInt();
                int qtd  = arq.readInt();
                info("Cabeçalho → ultimoID=" + ultimoID + " | qtd=" + qtd);

                arq.seek(8);

                while (arq.getFilePointer() < arq.length()) {

                    long    pos   = arq.getFilePointer();
                    boolean ativo = arq.readBoolean();
                    int     tam   = arq.readInt();
                    byte[]  dados = new byte[tam];
                    arq.readFully(dados);

                    if (ativo) {
                        Autor a = new Autor();
                        a.fromByteArray(dados);
                        ultimoValido = a.getId();
                        info("pos=" + pos + " | id=" + a.getId() + " | nome=" + a.getNome());
                    } else {
                        info("pos=" + pos + " | [REMOVIDO]");
                    }
                }
            }

            info("Último ID válido encontrado: " + ultimoValido);

            // =================================================================
            // 2. READ — leitura via hash dos registros existentes
            // =================================================================
            secao("2. READ via HASH (dados existentes)");

            // lê o último autor válido
            Autor ultimo = dao.read(ultimoValido);
            assertNaoNulo(ultimo, "read(id=" + ultimoValido + ") — último autor válido");
            info("Último autor: id=" + ultimo.getId() + " | " + ultimo.getNome());

            // lê os primeiros registros (até 5 ou até ultimoID)
            info("\nPrimeiros registros:");
            for (int i = 1; i <= Math.min(5, ultimoID); i++) {
                Autor a = dao.read(i);
                if (a != null) {
                    ok("id=" + i + " → " + a.getNome());
                } else {
                    info("id=" + i + " → não encontrado (removido ou inexistente)");
                }
            }

            // varredura completa até ultimoID
            info("\nVarredura completa (id 1 até " + ultimoID + "):");
            for (int i = 1; i <= ultimoID; i++) {
                Autor a = dao.read(i);
                System.out.println("  " + i + " → " + (a != null ? "OK | " + a.getNome() : "NULL"));
            }

            // busca por ID inexistente
            Autor naoExiste = dao.read(99999);
            assertNulo(naoExiste, "read(id=99999) deve retornar null");

            // =================================================================
            // 3. READ ALL
            // =================================================================
            secao("3. READ ALL");
            List<Autor> todos = dao.readAll();
            info("Total de autores ativos: " + todos.size());
            todos.forEach(a -> info("  id=" + a.getId() + " | " + a.getNome()));

            // =================================================================
            // 4. CREATE — insere novos autores
            // =================================================================
            secao("4. CREATE — inserindo autores de teste");

            Autor novo1 = new Autor();
            novo1.setNome("Machado de Assis");

            Autor novo2 = new Autor();
            novo2.setNome("Clarice Lispector");

            int idNovo1 = dao.create(novo1);
            int idNovo2 = dao.create(novo2);

            ok("Criado → id=" + idNovo1 + " | " + novo1.getNome());
            ok("Criado → id=" + idNovo2 + " | " + novo2.getNome());

            // confirma leitura imediata
            Autor lido1 = dao.read(idNovo1);
            assertNaoNulo(lido1, "read(id=" + idNovo1 + ") após create");
            info("Lido: " + lido1.getNome());

            // =================================================================
            // 5. UPDATE
            // =================================================================
            secao("5. UPDATE");

            Autor paraAtualizar = dao.read(idNovo1);
            paraAtualizar.setNome("Machado de Assis - Ed. Atualizada");

            boolean upOk = dao.update(paraAtualizar);
            assertTrue(upOk, "update(id=" + idNovo1 + ") deve retornar true");

            Autor aposUpdate = dao.read(idNovo1);
            assertNaoNulo(aposUpdate, "read após update não deve ser null");
            assertEqual(aposUpdate.getNome(), "Machado de Assis - Ed. Atualizada",
                        "nome após update");
            info("Após update: " + aposUpdate.getNome());

            // update em ID inexistente
            Autor fantasma = new Autor();
            fantasma.setId(99999);
            fantasma.setNome("Fantasma");
            boolean upFalso = dao.update(fantasma);
            assertFalse(upFalso, "update(id=99999) deve retornar false");

            // =================================================================
            // 6. DELETE
            // =================================================================
            secao("6. DELETE");

            boolean delOk = dao.delete(idNovo2);
            assertTrue(delOk, "delete(id=" + idNovo2 + ") deve retornar true");

            Autor aposDelete = dao.read(idNovo2);
            assertNulo(aposDelete, "read após delete deve retornar null");

            // delete em ID inexistente
            boolean delFalso = dao.delete(99999);
            assertFalse(delFalso, "delete(id=99999) deve retornar false");

            // delete duplo
            boolean delDuplo = dao.delete(idNovo2);
            assertFalse(delDuplo, "segundo delete(id=" + idNovo2 + ") deve retornar false");

            // =================================================================
            // 7. RECONSTRUÇÃO APÓS OPERAÇÕES (simula reinício)
            // =================================================================
            secao("7. RECONSTRUÇÃO APÓS OPERAÇÕES (simula reinício da aplicação)");

            dao.reconstruirHash();
            ok("Hash reconstruído após operações");

            // confirma que os dados persistem corretamente
            Autor aposRec = dao.read(idNovo1);
            assertNaoNulo(aposRec, "read(id=" + idNovo1 + ") após reconstrução");
            info("Recuperado após reconstrução: " + aposRec.getNome());

            Autor deletadoAposRec = dao.read(idNovo2);
            assertNulo(deletadoAposRec, "registro deletado não deve aparecer após reconstrução");

            // =================================================================
            // 8. ESTADO FINAL
            // =================================================================
            secao("8. ESTADO FINAL — TODOS OS AUTORES ATIVOS");

            List<Autor> final_ = dao.readAll();
            info("Total de autores ativos: " + final_.size());
            final_.forEach(a -> info("  id=" + a.getId() + " | " + a.getNome()));

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
        ok(descricao + " → não nulo ✔");
    }

    private static void assertNulo(Object obj, String descricao) {
        if (obj != null)
            throw new AssertionError(descricao + " → esperava null, recebeu " + obj);
        ok(descricao + " → null ✔");
    }

    private static void assertTrue(boolean valor, String descricao) {
        if (!valor)
            throw new AssertionError(descricao + " → esperava true, recebeu false");
        ok(descricao + " → true ✔");
    }

    private static void assertFalse(boolean valor, String descricao) {
        if (valor)
            throw new AssertionError(descricao + " → esperava false, recebeu true");
        ok(descricao + " → false ✔");
    }

    private static void assertEqual(String real, String esperado, String descricao) {
        if (!real.equals(esperado))
            throw new AssertionError(
                descricao + " → esperava '" + esperado + "', recebeu '" + real + "'");
        ok(descricao + " → '" + real + "' ✔");
    }
}