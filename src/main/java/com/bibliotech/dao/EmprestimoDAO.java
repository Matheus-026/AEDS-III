package com.bibliotech.dao;

import com.bibliotech.model.Emprestimo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * EmprestimoDAO
 *
 * Índices mantidos em disco:
 *
 *  hashPK  (emprestimos_dir.hash / emprestimos_bucket.hash)
 *      idEmprestimo (int) → posição no arquivo .dat (long)
 *      Usado em: read, update, delete — O(1)
 *
 *  hashUsuario  (emp_usuario_dir.hash / emp_usuario_bucket.hash)
 *      idUsuario (int) → lista de posições no .dat (long[])
 *      Relacionamento 1:N: um usuário pode ter vários empréstimos.
 *      Usado em: buscarPorUsuario — O(1) no índice
 *
 *  hashLivro  (emp_livro_dir.hash / emp_livro_bucket.hash)
 *      idLivro (int) → lista de posições no .dat (long[])
 *      Relacionamento 1:N: um livro pode ter vários empréstimos.
 *      Usado em: buscarPorLivro — O(1) no índice
 */
public class EmprestimoDAO {

    private RandomAccessFile arquivo;

    /** Índice primário: idEmprestimo → posição no .dat */
    private HashExtensivel hashPK;

    /** Índice secundário 1:N: idUsuario → [posições no .dat] */
    private HashExtensivel hashUsuario;

    /** Índice secundário 1:N: idLivro → [posições no .dat] */
    private HashExtensivel hashLivro;


    // CONSTRUTOR
    public EmprestimoDAO() throws IOException {

        new File("data").mkdirs();
        new File("data/diretorios").mkdirs();
        new File("data/buckets").mkdirs();

        arquivo = new RandomAccessFile("data/emprestimos.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // ultimoId
            arquivo.writeInt(0); // quantidadeRegistros
        }

        hashPK      = new HashExtensivel("emprestimos");
        hashUsuario = new HashExtensivel("emp_usuario");
        hashLivro   = new HashExtensivel("emp_livro");

        if (indiceEstaVazio("emprestimos")) {
            reconstruirIndices();
        }
    }

 
    // VERIFICAR ÍNDICES
    private boolean indiceEstaVazio(String nome) {
        File dir    = new File("data/diretorios/" + nome + "_dir.hash");
        File bucket = new File("data/buckets/"    + nome + "_bucket.hash");
        return !dir.exists()    || dir.length()    <= 4
            || !bucket.exists() || bucket.length() == 0;
    }

   
    // CREATE
    /**
     * Insere um novo empréstimo e atualiza os três índices.
     */
    public int create(Emprestimo emprestimo) throws IOException {

        arquivo.seek(0);
        int ultimoID  = arquivo.readInt();
        int quantidade = arquivo.readInt();

        ultimoID++;
        quantidade++;
        emprestimo.setId(ultimoID);

        // validação PK duplicada
        if (hashPK.buscar(emprestimo.getId()) != -1) {
            throw new IllegalArgumentException(
                "PK duplicada: já existe empréstimo com id=" + emprestimo.getId());
        }

        long   pos   = arquivo.length();
        byte[] dados = emprestimo.toByteArray();

        arquivo.seek(pos);
        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // ── atualiza os três índices 
        hashPK.inserir(emprestimo.getId(), pos);
        hashUsuario.inserirLista(emprestimo.getIdUsuario(), pos);
        hashLivro.inserirLista(emprestimo.getIdLivro(), pos);

        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);

        return emprestimo.getId();
    }


    // READ (por PK)
    /**
     * Busca empréstimo pelo ID usando o índice primário — O(1).
     */
    public Emprestimo read(int id) throws IOException {

        long pos = hashPK.buscar(id);

        if (pos == -1)
            return null;

        return lerNaPosicao(pos);
    }


    // LISTAR TODOS
    public List<Emprestimo> listAll() throws IOException {

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

 
    // UPDATE
    /**
     * Atualiza um empréstimo existente.
     *
     * Se o novo conteúdo couber no espaço original → sobrescreve in-place
     * atualizando corretamente o campo tamanho.
     * Caso contrário → invalida o antigo, grava no fim e atualiza os índices.
     */
    public boolean update(Emprestimo novo) throws IOException {

        long pos = hashPK.buscar(novo.getId());

        if (pos == -1)
            return false;

        arquivo.seek(pos);
        boolean ativo        = arquivo.readBoolean();
        int     tamanhoAntigo = arquivo.readInt();

        if (!ativo)
            return false;

        // lê os FKs antes de sobrescrever para sincronizar índices se mudarem
        Emprestimo antigo = lerNaPosicao(pos);
        int idUsuarioAntigo = antigo.getIdUsuario();
        int idLivroAntigo   = antigo.getIdLivro();

        byte[] novosDados = novo.toByteArray();

        if (novosDados.length <= tamanhoAntigo) {
            // ── in-place: atualiza tamanho e dados
            arquivo.seek(pos + 1);               // pula boolean (1 byte)
            arquivo.writeInt(novosDados.length); // atualiza tamanho correto
            arquivo.write(novosDados);

            // sincroniza índices 1:N se as FKs mudaram
            sincronizarIndices1N(idUsuarioAntigo, idLivroAntigo,
                                 novo.getIdUsuario(), novo.getIdLivro(),
                                 pos, pos);

        } else {
            // ── move para o fim 
            arquivo.seek(pos);
            arquivo.writeBoolean(false);

            long novaPos = arquivo.length();
            arquivo.seek(novaPos);
            arquivo.writeBoolean(true);
            arquivo.writeInt(novosDados.length);
            arquivo.write(novosDados);

            // atualiza índice primário
            hashPK.remover(novo.getId());
            hashPK.inserir(novo.getId(), novaPos);

            // atualiza índices 1:N
            sincronizarIndices1N(idUsuarioAntigo, idLivroAntigo,
                                 novo.getIdUsuario(), novo.getIdLivro(),
                                 pos, novaPos);
        }

        return true;
    }

    // DELETE
    /**
     * Exclusão lógica: marca como inativo e remove dos três índices.
     */
    public boolean delete(int id) throws IOException {

        long pos = hashPK.buscar(id);

        if (pos == -1)
            return false;

        arquivo.seek(pos);
        boolean ativo = arquivo.readBoolean();

        if (!ativo)
            return false;

        // lê as FKs antes de invalidar
        Emprestimo e = lerNaPosicao(pos);

        arquivo.seek(pos);
        arquivo.writeBoolean(false); // exclusão lógica

        // remove dos três índices
        hashPK.remover(id);
        hashUsuario.removerLista(e.getIdUsuario(), pos);
        hashLivro.removerLista(e.getIdLivro(), pos);

        return true;
    }


    // BUSCAR POR USUÁRIO — índice secundário 1:N
    /**
     * Retorna todos os empréstimos de um usuário consultando o índice secundário.
     *
     * Fluxo:
     *   1. hashUsuario.buscarLista(idUsuario) → lista de posições no .dat
     *   2. Para cada posição, lê o registro diretamente por offset (seek)
     */
    public List<Emprestimo> buscarPorUsuario(int idUsuario) throws IOException {

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

 
    // BUSCAR POR LIVRO — índice secundário 1:N
    /**
     * Retorna todos os empréstimos de um livro consultando o índice secundário.
     *
     * Fluxo:
     *   1. hashLivro.buscarLista(idLivro) → lista de posições no .dat
     *   2. Para cada posição, lê o registro diretamente por offset (seek).
     */
    public List<Emprestimo> buscarPorLivro(int idLivro) throws IOException {

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


    // HELPERS PRIVADOS
    /**
     * Lê e desserializa um Emprestimo diretamente na posição informada.
     * Retorna null se o registro estiver inativo ou posição inválida.
     */
    private Emprestimo lerNaPosicao(long pos) throws IOException {

        if (pos < 0 || pos >= arquivo.length())
            return null;

        arquivo.seek(pos);
        boolean ativo   = arquivo.readBoolean();
        int     tamanho = arquivo.readInt();

        if (!ativo)
            return null;

        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);

        Emprestimo e = new Emprestimo();
        e.fromByteArray(dados);
        return e;
    }

    /**
     * Sincroniza os índices 1:N quando um update muda as FKs ou a posição.
     *
     * @param idUsuarioAntigo  FK usuário antes da alteração
     * @param idLivroAntigo    FK livro antes da alteração
     * @param idUsuarioNovo    FK usuário depois da alteração
     * @param idLivroNovo      FK livro depois da alteração
     * @param posAntiga        posição antiga no .dat
     * @param posNova          posição nova no .dat (igual à antiga se in-place)
     */
    private void sincronizarIndices1N(int idUsuarioAntigo, int idLivroAntigo,
                                      int idUsuarioNovo,   int idLivroNovo,
                                      long posAntiga,      long posNova)
            throws IOException {

        // ── índice usuário ────────────────────────────────────────────────────
        if (idUsuarioAntigo != idUsuarioNovo || posAntiga != posNova) {
            hashUsuario.removerLista(idUsuarioAntigo, posAntiga);
            hashUsuario.inserirLista(idUsuarioNovo, posNova);
        }

        // ── índice livro ──────────────────────────────────────────────────────
        if (idLivroAntigo != idLivroNovo || posAntiga != posNova) {
            hashLivro.removerLista(idLivroAntigo, posAntiga);
            hashLivro.inserirLista(idLivroNovo, posNova);
        }
    }


    // RECONSTRUIR ÍNDICES
    /**
     * Reconstrói os três índices percorrendo o arquivo .dat.
     * Chamado automaticamente se os índices estiverem ausentes.
     */
    public void reconstruirIndices() throws IOException {

        zerarArquivo("data/diretorios/emprestimos_dir.hash");
        zerarArquivo("data/buckets/emprestimos_bucket.hash");
        zerarArquivo("data/diretorios/emp_usuario_dir.hash");
        zerarArquivo("data/buckets/emp_usuario_bucket.hash");
        zerarArquivo("data/diretorios/emp_livro_dir.hash");
        zerarArquivo("data/buckets/emp_livro_bucket.hash");

        hashPK      = new HashExtensivel("emprestimos");
        hashUsuario = new HashExtensivel("emp_usuario");
        hashLivro   = new HashExtensivel("emp_livro");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long    pos    = arquivo.getFilePointer();
            boolean ativo  = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (ativo) {
                Emprestimo e = new Emprestimo();
                e.fromByteArray(dados);

                hashPK.inserir(e.getId(), pos);
                hashUsuario.inserirLista(e.getIdUsuario(), pos);
                hashLivro.inserirLista(e.getIdLivro(), pos);
            }
        }

        System.out.println("[EmprestimoDAO] Índices reconstruídos com sucesso.");
    }

    // mantém compatibilidade com código legado que chama reconstruirHash()
    public void reconstruirHash() throws IOException {
        reconstruirIndices();
    }

 
    // UTILITÁRIOS
    private void zerarArquivo(String caminho) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(caminho, "rw")) {
            f.setLength(0);
        }
    }

    public void fechar() throws IOException {
        arquivo.close();
        hashPK.fechar();
        hashUsuario.fechar();
        hashLivro.fechar();
    }
}