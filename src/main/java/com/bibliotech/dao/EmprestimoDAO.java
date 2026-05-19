package com.bibliotech.dao;

import com.bibliotech.model.Emprestimo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EmprestimoDAO — Relacionamento N:N entre Usuario e Livro
 *
 * Um usuário pode tomar emprestado vários livros.
 * Um livro pode ser emprestado a vários usuários.
 * A tabela Emprestimo é a tabela intermediária desse relacionamento N:N.
 *
 * Índices mantidos em disco:
 *
 *  hashPK  (emprestimos_dir / emprestimos_bucket)
 *      idEmprestimo (int) → posição no .dat
 *      Índice primário sequencial — O(1)
 *
 *  hashChaveComposta  (emp_composta_dir / emp_composta_bucket)
 *      chaveInt = idUsuario * 31 + idLivro → lista de posições no .dat
 *      Garante unicidade do par (idUsuario, idLivro) ativo.
 *      Permite busca direta pelo par sem varredura — O(1)
 *
 *  hashUsuario  (emp_usuario_dir / emp_usuario_bucket)
 *      idUsuario (int) → lista de posições no .dat
 *      Navegação N:N: todos os livros de um usuário — O(k)
 *
 *  hashLivro  (emp_livro_dir / emp_livro_bucket)
 *      idLivro (int) → lista de posições no .dat
 *      Navegação N:N: todos os usuários de um livro — O(k)
 */
public class EmprestimoDAO {

    private RandomAccessFile arquivo;

    /** Índice primário: idEmprestimo → posição no .dat */
    private HashExtensivel hashPK;

    /**
     * Índice de chave composta: hash(idUsuario, idLivro) → lista de posições no .dat
     * Garante unicidade do par ativo e permite busca direta pelo par.
     */
    private HashExtensivel hashChaveComposta;

    /** Índice N:N lado usuário: idUsuario → [posições no .dat] */
    private HashExtensivel hashUsuario;

    /** Índice N:N lado livro: idLivro → [posições no .dat] */
    private HashExtensivel hashLivro;

    private final Object lock = new Object();

    // -------------------------------------------------------------------------
    // CONSTRUTOR
    // -------------------------------------------------------------------------
    public EmprestimoDAO() throws IOException {

        new File("data").mkdirs();
        new File("data/diretorios").mkdirs();
        new File("data/buckets").mkdirs();

        arquivo = new RandomAccessFile("data/emprestimos.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0);
            arquivo.writeInt(0);
        }

        hashPK            = new HashExtensivel("emprestimos");
        hashChaveComposta = new HashExtensivel("emp_composta");
        hashUsuario       = new HashExtensivel("emp_usuario");
        hashLivro         = new HashExtensivel("emp_livro");

        if (indiceEstaVazio("emprestimos")) {
            reconstruirIndices();
        }
    }

    // -------------------------------------------------------------------------
    // CHAVE COMPOSTA
    // -------------------------------------------------------------------------

    /**
     * Deriva uma chave int a partir do par (idUsuario, idLivro).
     * Usada como chave no hashChaveComposta.
     * Colisões são tratadas verificando o registro na posição retornada.
     */
    private int hashChaveInt(int idUsuario, int idLivro) {
        return idUsuario * 31 + idLivro;
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * Registra um novo empréstimo (par idUsuario × idLivro).
     *
     * Validação de chave composta:
     *  Não permite dois empréstimos ATIVOS do mesmo par (idUsuario, idLivro).
     *  Um novo empréstimo só é permitido após devolução do anterior.
     *
     * @throws IllegalStateException se já existir empréstimo ativo para o par.
     */
    public int create(Emprestimo emprestimo) throws IOException {
        synchronized (lock) {

            // ── validação da chave composta ───────────────────────────────────
            Emprestimo ativo = buscarAtivoPorParInterno(
                emprestimo.getIdUsuario(), emprestimo.getIdLivro());

            if (ativo != null) {
                throw new IllegalStateException(
                    "Já existe empréstimo ativo para o par " +
                    "(idUsuario=" + emprestimo.getIdUsuario() +
                    ", idLivro="  + emprestimo.getIdLivro()   + "). " +
                    "O livro deve ser devolvido antes de um novo empréstimo.");
            }

            arquivo.seek(0);
            int ultimoID   = arquivo.readInt();
            int quantidade = arquivo.readInt();

            ultimoID++;
            quantidade++;
            emprestimo.setId(ultimoID);

            long   pos   = arquivo.length();
            byte[] dados = emprestimo.toByteArray();

            arquivo.seek(pos);
            arquivo.writeBoolean(true);
            arquivo.writeInt(dados.length);
            arquivo.write(dados);

            // ── atualiza os quatro índices ────────────────────────────────────
            hashPK.inserir(emprestimo.getId(), pos);

            int chaveInt = hashChaveInt(
                emprestimo.getIdUsuario(), emprestimo.getIdLivro());
            hashChaveComposta.inserirLista(chaveInt, pos);

            hashUsuario.inserirLista(emprestimo.getIdUsuario(), pos);
            hashLivro.inserirLista(emprestimo.getIdLivro(), pos);

            arquivo.seek(0);
            arquivo.writeInt(ultimoID);
            arquivo.writeInt(quantidade);

            return emprestimo.getId();
        }
    }

    // -------------------------------------------------------------------------
    // READ (por PK)
    // -------------------------------------------------------------------------
    public Emprestimo read(int id) throws IOException {
        synchronized (lock) {
            long pos = hashPK.buscar(id);
            if (pos == -1) return null;
            return lerNaPosicao(pos);
        }
    }

    // -------------------------------------------------------------------------
    // READ (por chave composta)
    // -------------------------------------------------------------------------

    /**
     * Busca o empréstimo ATIVO para o par (idUsuario, idLivro).
     * Retorna null se não houver empréstimo ativo para esse par.
     *
     * Permite registrar devolução sem precisar do idEmprestimo,
     * e verificar disponibilidade de um livro para um usuário específico.
     */
    public Emprestimo buscarAtivoPorPar(int idUsuario, int idLivro) throws IOException {
        synchronized (lock) {
            return buscarAtivoPorParInterno(idUsuario, idLivro);
        }
    }

    // versão sem synchronized para uso interno dentro de métodos já sincronizados
    private Emprestimo buscarAtivoPorParInterno(int idUsuario, int idLivro)
            throws IOException {

        int        chaveInt = hashChaveInt(idUsuario, idLivro);
        List<Long> posicoes = hashChaveComposta.buscarLista(chaveInt);

        for (long pos : posicoes) {
            Emprestimo e = lerNaPosicao(pos);
            if (e == null) continue;

            // confirma o par exato (resolve colisões do hashChaveInt)
            if (e.getIdUsuario() != idUsuario || e.getIdLivro() != idLivro) continue;

            // só retorna se estiver ativo
            if (!"Devolvido".equals(e.getStatus())) return e;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // LIST ALL
    // -------------------------------------------------------------------------
    public List<Emprestimo> listAll() throws IOException {
        synchronized (lock) {
            List<Emprestimo> lista = new ArrayList<>();
            arquivo.seek(8);

            while (arquivo.getFilePointer() < arquivo.length()) {
                boolean ativo   = arquivo.readBoolean();
                int     tamanho = arquivo.readInt();

                if (ativo) {
                    byte[] dados = new byte[tamanho];
                    arquivo.readFully(dados);
                    Emprestimo e = new Emprestimo();
                    e.fromByteArray(dados);
                    e.atualizarStatus();
                    lista.add(e);
                } else {
                    arquivo.skipBytes(tamanho);
                }
            }
            return lista;
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------
    public boolean update(Emprestimo novo) throws IOException {
        synchronized (lock) {
            long pos = hashPK.buscar(novo.getId());
            if (pos == -1) return false;

            arquivo.seek(pos);
            boolean ativo        = arquivo.readBoolean();
            int     tamanhoAntigo = arquivo.readInt();
            if (!ativo) return false;

            Emprestimo antigo  = lerNaPosicao(pos);
            byte[]     novoDados = novo.toByteArray();

            if (novoDados.length <= tamanhoAntigo) {
                arquivo.seek(pos + 1);
                arquivo.writeInt(novoDados.length);
                arquivo.write(novoDados);
                sincronizarIndices(antigo, novo, pos, pos);
            } else {
                arquivo.seek(pos);
                arquivo.writeBoolean(false);

                long novaPos = arquivo.length();
                arquivo.seek(novaPos);
                arquivo.writeBoolean(true);
                arquivo.writeInt(novoDados.length);
                arquivo.write(novoDados);

                hashPK.remover(novo.getId());
                hashPK.inserir(novo.getId(), novaPos);
                sincronizarIndices(antigo, novo, pos, novaPos);
            }

            return true;
        }
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------
    public boolean delete(int id) throws IOException {
        synchronized (lock) {
            long pos = hashPK.buscar(id);
            if (pos == -1) return false;

            arquivo.seek(pos);
            boolean ativo = arquivo.readBoolean();
            if (!ativo) return false;

            Emprestimo e = lerNaPosicao(pos);

            arquivo.seek(pos);
            arquivo.writeBoolean(false);

            hashPK.remover(id);

            int chaveInt = hashChaveInt(e.getIdUsuario(), e.getIdLivro());
            hashChaveComposta.removerLista(chaveInt, pos);
            hashUsuario.removerLista(e.getIdUsuario(), pos);
            hashLivro.removerLista(e.getIdLivro(), pos);

            return true;
        }
    }

    // -------------------------------------------------------------------------
    // NAVEGAÇÃO N:N — lado Usuário → Livros
    // -------------------------------------------------------------------------

    /**
     * Retorna todos os empréstimos de um usuário.
     * Navegação N:N: Usuario → Livros emprestados.
     * Complexidade: O(k), sem varredura linear.
     */
    public List<Emprestimo> buscarPorUsuario(int idUsuario) throws IOException {
        synchronized (lock) {
            List<Emprestimo> lista    = new ArrayList<>();
            List<Long>       posicoes = hashUsuario.buscarLista(idUsuario);

            for (long pos : posicoes) {
                Emprestimo e = lerNaPosicao(pos);
                if (e != null) {
                    e.atualizarStatus();
                    lista.add(e);
                }
            }
            return lista;
        }
    }

    // -------------------------------------------------------------------------
    // NAVEGAÇÃO N:N — lado Livro → Usuários
    // -------------------------------------------------------------------------

    /**
     * Retorna todos os empréstimos de um livro.
     * Navegação N:N: Livro → Usuários que já pegaram emprestado.
     * Complexidade: O(k), sem varredura linear.
     */
    public List<Emprestimo> buscarPorLivro(int idLivro) throws IOException {
        synchronized (lock) {
            List<Emprestimo> lista    = new ArrayList<>();
            List<Long>       posicoes = hashLivro.buscarLista(idLivro);

            for (long pos : posicoes) {
                Emprestimo e = lerNaPosicao(pos);
                if (e != null) {
                    e.atualizarStatus();
                    lista.add(e);
                }
            }
            return lista;
        }
    }

    // -------------------------------------------------------------------------
    // DEVOLUÇÃO POR PAR (sem precisar do idEmprestimo)
    // -------------------------------------------------------------------------

    /**
     * Registra a devolução usando apenas o par (idUsuario, idLivro).
     * Busca o empréstimo ativo via índice de chave composta e marca como Devolvido.
     *
     * @return o empréstimo atualizado, ou null se não havia empréstimo ativo.
     */
    public Emprestimo registrarDevolucao(int idUsuario, int idLivro) throws IOException {
        synchronized (lock) {
            Emprestimo ativo = buscarAtivoPorParInterno(idUsuario, idLivro);
            if (ativo == null) return null;

            ativo.setStatus("Devolvido");

            long pos = hashPK.buscar(ativo.getId());
            if (pos == -1) return null;

            arquivo.seek(pos);
            boolean eAtivo    = arquivo.readBoolean();
            int tamanhoAntigo = arquivo.readInt();
            if (!eAtivo) return null;

            Emprestimo antigo  = lerNaPosicao(pos);
            byte[]     novoDados = ativo.toByteArray();

            if (novoDados.length <= tamanhoAntigo) {
                arquivo.seek(pos + 1);
                arquivo.writeInt(novoDados.length);
                arquivo.write(novoDados);
                sincronizarIndices(antigo, ativo, pos, pos);
            } else {
                arquivo.seek(pos);
                arquivo.writeBoolean(false);
                long novaPos = arquivo.length();
                arquivo.seek(novaPos);
                arquivo.writeBoolean(true);
                arquivo.writeInt(novoDados.length);
                arquivo.write(novoDados);
                hashPK.remover(ativo.getId());
                hashPK.inserir(ativo.getId(), novaPos);
                sincronizarIndices(antigo, ativo, pos, novaPos);
            }

            return ativo;
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private Emprestimo lerNaPosicao(long pos) throws IOException {
        if (pos < 0 || pos >= arquivo.length()) return null;

        arquivo.seek(pos);
        boolean ativo   = arquivo.readBoolean();
        int     tamanho = arquivo.readInt();
        if (!ativo) return null;

        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);

        Emprestimo e = new Emprestimo();
        e.fromByteArray(dados);
        return e;
    }

    /**
     * Sincroniza os três índices secundários quando um update
     * muda as FKs ou a posição física do registro.
     */
    private void sincronizarIndices(Emprestimo antigo, Emprestimo novo,
                                    long posAntiga, long posNova)
            throws IOException {

        int chaveIntAntigo = hashChaveInt(antigo.getIdUsuario(), antigo.getIdLivro());
        int chaveIntNovo   = hashChaveInt(novo.getIdUsuario(),   novo.getIdLivro());

        boolean parMudou = antigo.getIdUsuario() != novo.getIdUsuario()
                        || antigo.getIdLivro()   != novo.getIdLivro();
        boolean moveu    = posAntiga != posNova;

        if (parMudou || moveu) {
            hashChaveComposta.removerLista(chaveIntAntigo, posAntiga);
            hashChaveComposta.inserirLista(chaveIntNovo,   posNova);
        }

        if (antigo.getIdUsuario() != novo.getIdUsuario() || moveu) {
            hashUsuario.removerLista(antigo.getIdUsuario(), posAntiga);
            hashUsuario.inserirLista(novo.getIdUsuario(),   posNova);
        }

        if (antigo.getIdLivro() != novo.getIdLivro() || moveu) {
            hashLivro.removerLista(antigo.getIdLivro(), posAntiga);
            hashLivro.inserirLista(novo.getIdLivro(),   posNova);
        }
    }

    // -------------------------------------------------------------------------
    // RECONSTRUIR ÍNDICES
    // -------------------------------------------------------------------------
    public void reconstruirIndices() throws IOException {
        synchronized (lock) {
            zerarArquivo("data/diretorios/emprestimos_dir.hash");
            zerarArquivo("data/buckets/emprestimos_bucket.hash");
            zerarArquivo("data/diretorios/emp_composta_dir.hash");
            zerarArquivo("data/buckets/emp_composta_bucket.hash");
            zerarArquivo("data/diretorios/emp_usuario_dir.hash");
            zerarArquivo("data/buckets/emp_usuario_bucket.hash");
            zerarArquivo("data/diretorios/emp_livro_dir.hash");
            zerarArquivo("data/buckets/emp_livro_bucket.hash");

            hashPK            = new HashExtensivel("emprestimos");
            hashChaveComposta = new HashExtensivel("emp_composta");
            hashUsuario       = new HashExtensivel("emp_usuario");
            hashLivro         = new HashExtensivel("emp_livro");

            arquivo.seek(8);

            while (arquivo.getFilePointer() < arquivo.length()) {
                long    pos   = arquivo.getFilePointer();
                boolean ativo = arquivo.readBoolean();
                int     tam   = arquivo.readInt();
                byte[]  dados = new byte[tam];
                arquivo.readFully(dados);

                if (ativo) {
                    Emprestimo e = new Emprestimo();
                    e.fromByteArray(dados);

                    hashPK.inserir(e.getId(), pos);

                    int chaveInt = hashChaveInt(e.getIdUsuario(), e.getIdLivro());
                    hashChaveComposta.inserirLista(chaveInt, pos);

                    hashUsuario.inserirLista(e.getIdUsuario(), pos);
                    hashLivro.inserirLista(e.getIdLivro(), pos);
                }
            }

            System.out.println("[EmprestimoDAO] Índices reconstruídos com sucesso.");
        }
    }

    public void reconstruirHash() throws IOException {
        reconstruirIndices();
    }

    // -------------------------------------------------------------------------
    // UTILITÁRIOS
    // -------------------------------------------------------------------------
    private boolean indiceEstaVazio(String nome) {
        File dir    = new File("data/diretorios/" + nome + "_dir.hash");
        File bucket = new File("data/buckets/"    + nome + "_bucket.hash");
        return !dir.exists()    || dir.length()    <= 4
            || !bucket.exists() || bucket.length() == 0;
    }

    private void zerarArquivo(String caminho) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(caminho, "rw")) {
            f.setLength(0);
        }
    }

    public void fechar() throws IOException {
        arquivo.close();
        hashPK.fechar();
        hashChaveComposta.fechar();
        hashUsuario.fechar();
        hashLivro.fechar();
    }
}