package com.bibliotech.dao;

import com.bibliotech.model.Livro;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LivroDAO {

    private RandomAccessFile arquivo;
    private HashExtensivel hash;

    public LivroDAO() throws IOException {
        arquivo = new RandomAccessFile("data/livros.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // último ID
            arquivo.writeInt(0); // quantidade
        }

        hash = new HashExtensivel("livros");

        if (hashEstaVazio()) {
            reconstruirHash();
        }
    }

    // VERIFICAR HASH
    private boolean hashEstaVazio() {

        File dir = new File("data/diretorios/livros_dir.hash");
        File bucket = new File("data/buckets/livros_bucket.hash");

        return !dir.exists() || dir.length() <= 4 ||
               !bucket.exists() || bucket.length() == 0;
    }

   
    // CREATE (COM HASH)
    public int create(Livro livro) throws IOException {

        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int quantidade = arquivo.readInt();

        ultimoID++;
        quantidade++;

        livro.setId(ultimoID);

        long pos = arquivo.length();
        arquivo.seek(pos);

        byte[] dados = livro.toByteArray();

        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // 🔥 HASH
        hash.inserir(livro.getId(), pos);

        // atualiza cabeçalho
        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);

        return livro.getId();
    }

    // READ (HASH)
    public Livro read(int id) throws IOException {

        long pos = hash.buscar(id);

        if (pos == -1)
            return null;

        arquivo.seek(pos);

        boolean ativo = arquivo.readBoolean();
        int tamanho = arquivo.readInt();

        if (!ativo)
            return null;

        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);

        Livro l = new Livro();
        l.fromByteArray(dados);

        return l;
    }

    // DELETE (COM HASH)
    public boolean delete(int id) throws IOException {

        long pos = hash.buscar(id);

        if (pos == -1)
            return false;

        arquivo.seek(pos);

        boolean ativo = arquivo.readBoolean();

        if (!ativo)
            return false;

        // marca como removido
        arquivo.seek(pos);
        arquivo.writeBoolean(false);

        // remove do hash
        hash.remover(id);

        return true;
    }

    // UPDATE (COM HASH)
    public boolean update(Livro novoLivro) throws IOException {

        long pos = hash.buscar(novoLivro.getId());

        if (pos == -1)
            return false;

        arquivo.seek(pos);

        boolean ativo = arquivo.readBoolean();
        int tamanhoAntigo = arquivo.readInt();

        if (!ativo)
            return false;

        byte[] novosDados = novoLivro.toByteArray();

        // cabe no mesmo espaço → SOBRESCREVE
        if (novosDados.length <= tamanhoAntigo) {

            arquivo.seek(pos + 5); // boolean + int
            arquivo.write(novosDados);

            return true;
        }

        // não cabe → move
        arquivo.seek(pos);
        arquivo.writeBoolean(false);

        long novaPos = arquivo.length();
        arquivo.seek(novaPos);

        arquivo.writeBoolean(true);
        arquivo.writeInt(novosDados.length);
        arquivo.write(novosDados);

        hash.remover(novoLivro.getId());
        hash.inserir(novoLivro.getId(), novaPos);

        return true;
    }


    // LISTAR TODOS
    public List<Livro> readAll() throws IOException {

        List<Livro> lista = new ArrayList<>();

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);
                lista.add(l);
            }
        }

        return lista;
    }


    // RELACIONAMENTO 1:N

    public List<Livro> buscarPorAutor(int idAutor) throws IOException {

        List<Livro> lista = new ArrayList<>();

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                if (l.getIdAutor() == idAutor) {
                    lista.add(l);
                }
            }
        }

        return lista;
    }

    // BUSCA AVANÇADA 
    public List<Livro> buscaAvancada(String titulo, String genero, float precoMin, float precoMax) throws IOException {

        List<Livro> resultados = new ArrayList<>();
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            if (ativo) {
                byte[] dados = new byte[tamanho];
                arquivo.readFully(dados);

                Livro l = new Livro();
                l.fromByteArray(dados);

                boolean ok = true;

                if (titulo != null && !titulo.isEmpty()) {
                    if (!l.getTitulo().toLowerCase().contains(titulo.toLowerCase()))
                        ok = false;
                }

                if (genero != null && !genero.isEmpty()) {
                    boolean generoOk = false;

                    for (String g : l.getGeneros()) {
                        if (g.toLowerCase().contains(genero.toLowerCase())) {
                            generoOk = true;
                            break;
                        }
                    }

                    if (!generoOk) ok = false;
                }

                if (precoMin >= 0 && l.getPreco() < precoMin)
                    ok = false;

                if (precoMax >= 0 && l.getPreco() > precoMax)
                    ok = false;

                if (ok) resultados.add(l);

            } else {
                arquivo.skipBytes(tamanho);
            }
        }

        return resultados;
    }


    // RECONSTRUIR HASH
    public void reconstruirHash() throws IOException {

        RandomAccessFile dir =
                new RandomAccessFile("data/diretorios/livros_dir.hash", "rw");
        dir.setLength(0);
        dir.close();

        RandomAccessFile bucket =
                new RandomAccessFile("data/buckets/livros_bucket.hash", "rw");
        bucket.setLength(0);
        bucket.close();

        hash = new HashExtensivel("livros");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long pos = arquivo.getFilePointer();

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                hash.inserir(l.getId(), pos);
            }
        }

        System.out.println("Hash de livros reconstruído!");
    }
}