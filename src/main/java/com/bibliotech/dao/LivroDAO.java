package com.bibliotech.dao;

import com.bibliotech.model.Livro;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * LivroDAO
 *
 * Índices mantidos em disco:
 *
 *  hashPK  (livros_dir.hash / livros_bucket.hash)
 *      idLivro (int) → posição no arquivo .dat (long)
 *      Usado em: read, update, delete — O(1)
 *
 *  hashAutorLivros  (autorlivros_dir.hash / autorlivros_bucket.hash)
 *      idAutor (int) → lista de posições no .dat (long[])
 *      Relacionamento 1:N via Hash Extensível.
 *      Usado em: buscarPorAutor — O(1) no índice + acesso direto por offset
 */
public class LivroDAO {

    private RandomAccessFile arquivo;

    /** Índice primário: idLivro → posição no .dat */
    private HashExtensivel hashPK;

    /** Índice secundário 1:N: idAutor → [posições no .dat] */
    private HashExtensivel hashAutorLivros;

   
    // CONSTRUTOR   
    public LivroDAO() throws IOException {

        new File("data").mkdirs();
        new File("data/diretorios").mkdirs();
        new File("data/buckets").mkdirs();

        arquivo = new RandomAccessFile("data/livros.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // último ID gerado
            arquivo.writeInt(0); // quantidade de registros ativos
        }

        hashPK          = new HashExtensivel("livros");
        hashAutorLivros = new HashExtensivel("autorlivros");

        if (indiceEstaVazio("livros") || indiceEstaVazio("autorlivros")) {
            reconstruirIndices();
        }
    }

    
    // CREATE
    
    public int create(Livro livro) throws IOException {

        arquivo.seek(0);
        int ultimoID  = arquivo.readInt();
        int quantidade = arquivo.readInt();

        ultimoID++;
        quantidade++;
        livro.setId(ultimoID);

        // ── validação: PK duplicada ───────────────────────────────────────────
        if (hashPK.buscar(livro.getId()) != -1) {
            throw new IllegalArgumentException(
                "PK duplicada: já existe livro com id=" + livro.getId());
        }

        long   pos   = arquivo.length();
        byte[] dados = livro.toByteArray();

        arquivo.seek(pos);
        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // atualiza índice primário
        hashPK.inserir(livro.getId(), pos);

        // atualiza índice secundário 1:N
        hashAutorLivros.inserirLista(livro.getIdAutor(), pos);

        // atualiza cabeçalho
        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);

        return livro.getId();
    }

   
    // READ (por PK)
    
    public Livro read(int id) throws IOException {

        long pos = hashPK.buscar(id);

        if (pos == -1)
            return null;

        return lerNaPosicao(pos);
    }

   
    // UPDATE
    
    public boolean update(Livro novoLivro) throws IOException {

        long pos = hashPK.buscar(novoLivro.getId());

        if (pos == -1)
            return false;

        arquivo.seek(pos);
        boolean ativo        = arquivo.readBoolean();
        int     tamanhoAntigo = arquivo.readInt();

        if (!ativo)
            return false;

        // lê o autor antigo ANTES de sobrescrever
        int    idAutorAntigo = lerAutorDaPos(pos);
        byte[] novosDados    = novoLivro.toByteArray();

        if (novosDados.length <= tamanhoAntigo) {
            // ── in-place: atualiza tamanho e dados 
            arquivo.seek(pos + 1);               // pula boolean (1 byte)
            arquivo.writeInt(novosDados.length); // atualiza tamanho correto
            arquivo.write(novosDados);

            // atualiza índice 1:N se o autor mudou
            if (idAutorAntigo != novoLivro.getIdAutor()) {
                hashAutorLivros.removerLista(idAutorAntigo, pos);
                hashAutorLivros.inserirLista(novoLivro.getIdAutor(), pos);
            }

        } else {
            // ── move para o fim 
            arquivo.seek(pos);
            arquivo.writeBoolean(false); // invalida antigo

            long novaPos = arquivo.length();
            arquivo.seek(novaPos);
            arquivo.writeBoolean(true);
            arquivo.writeInt(novosDados.length);
            arquivo.write(novosDados);

            // atualiza índice primário
            hashPK.remover(novoLivro.getId());
            hashPK.inserir(novoLivro.getId(), novaPos);

            // atualiza índice secundário 1:N
            hashAutorLivros.removerLista(idAutorAntigo, pos);
            hashAutorLivros.inserirLista(novoLivro.getIdAutor(), novaPos);
        }

        return true;
    }

    
    // DELETE
    
    public boolean delete(int id) throws IOException {

        long pos = hashPK.buscar(id);

        if (pos == -1)
            return false;

        arquivo.seek(pos);
        boolean ativo = arquivo.readBoolean();

        if (!ativo)
            return false;

        // lê autor antes de invalidar o registro
        int idAutor = lerAutorDaPos(pos);

        arquivo.seek(pos);
        arquivo.writeBoolean(false); // exclusão lógica

        // remove dos índices
        hashPK.remover(id);
        hashAutorLivros.removerLista(idAutor, pos);

        return true;
    }

   
    // LISTAR TODOS
    public List<Livro> readAll() throws IOException {

        List<Livro> lista = new ArrayList<>();
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo   = arquivo.readBoolean();
            int     tamanho = arquivo.readInt();
            byte[]  dados   = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);
                lista.add(l);
            }
        }

        return lista;
    }

    
    // BUSCAR POR AUTOR — usa índice secundário 1:N
    public List<Livro> buscarPorAutor(int idAutor) throws IOException {

        List<Livro> resultado = new ArrayList<>();
        List<Long>  posicoes  = hashAutorLivros.buscarLista(idAutor);

        for (long pos : posicoes) {
            Livro l = lerNaPosicao(pos);
            if (l != null) resultado.add(l);
        }

        return resultado;
    }

   
    // BUSCA AVANÇADA (varredura com filtros combinados)
    public List<Livro> buscaAvancada(String titulo, String genero,
                                     float precoMin, float precoMax)
            throws IOException {

        List<Livro> resultados = new ArrayList<>();
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo   = arquivo.readBoolean();
            int     tamanho = arquivo.readInt();
            byte[]  dados   = new byte[tamanho];
            arquivo.readFully(dados);

            if (!ativo) continue;

            Livro l = new Livro();
            l.fromByteArray(dados);

            boolean ok = true;

            if (titulo != null && !titulo.isEmpty())
                if (!l.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                    ok = false;

            if (ok && genero != null && !genero.isEmpty()) {
                boolean achou = false;
                for (String g : l.getGeneros())
                    if (g.toLowerCase().contains(genero.toLowerCase())) {
                        achou = true;
                        break;
                    }
                if (!achou) ok = false;
            }

            if (ok && precoMin >= 0 && l.getPreco() < precoMin) ok = false;
            if (ok && precoMax >= 0 && l.getPreco() > precoMax) ok = false;

            if (ok) resultados.add(l);
        }

        return resultados;
    }

    
    // HELPERS PRIVADOS
    private Livro lerNaPosicao(long pos) throws IOException {

        if (pos < 0 || pos >= arquivo.length())
            return null;

        arquivo.seek(pos);
        boolean ativo   = arquivo.readBoolean();
        int     tamanho = arquivo.readInt();

        if (!ativo)
            return null;

        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);

        Livro l = new Livro();
        l.fromByteArray(dados);
        return l;
    }

    
    private int lerAutorDaPos(long pos) throws IOException {
        Livro l = lerNaPosicao(pos);
        return (l != null) ? l.getIdAutor() : -1;
    }

    
    // RECONSTRUIR ÍNDICES
    public void reconstruirIndices() throws IOException {

        zerarArquivo("data/diretorios/livros_dir.hash");
        zerarArquivo("data/buckets/livros_bucket.hash");
        zerarArquivo("data/diretorios/autorlivros_dir.hash");
        zerarArquivo("data/buckets/autorlivros_bucket.hash");

        hashPK          = new HashExtensivel("livros");
        hashAutorLivros = new HashExtensivel("autorlivros");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long    pos    = arquivo.getFilePointer();
            boolean ativo  = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                hashPK.inserir(l.getId(), pos);
                hashAutorLivros.inserirLista(l.getIdAutor(), pos);
            }
        }

        System.out.println("[LivroDAO] Índices reconstruídos com sucesso.");
    }


    // UTILITÁRIOS
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
        hashAutorLivros.fechar();
    }
}