package com.bibliotech.dao;

import com.bibliotech.model.Autor;
import java.io.*;

public class AutorDAO {

    private RandomAccessFile arquivo;
    private HashExtensivel hash;

    public AutorDAO() throws IOException {
        arquivo = new RandomAccessFile("data/autores.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0);
            arquivo.writeInt(0);
        }

        hash = new HashExtensivel("autores");
        reconstruirHash();
    }

    // =========================
    // CREATE
    // =========================
    public int create(Autor autor) throws IOException {
        arquivo.seek(0);

        int ultimoID = arquivo.readInt();
        int qtd = arquivo.readInt();

        ultimoID++;
        qtd++;

        autor.setId(ultimoID);

        long pos = arquivo.length();
        arquivo.seek(pos);

        byte[] dados = autor.toByteArray();

        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        hash.inserir(autor.getId(), pos);

        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(qtd);

        return autor.getId();
    }

    // =========================
    // READ (HASH)
    // =========================
    public Autor read(int id) throws IOException {
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

        Autor a = new Autor();
        a.fromByteArray(dados);

        return a;
    }

    // =========================
    // UPDATE (COM HASH 🚀)
    // =========================
    public boolean update(Autor novo) throws IOException {
        long pos = hash.buscar(novo.getId());

        if (pos == -1)
            return false;

        arquivo.seek(pos);

        boolean ativo = arquivo.readBoolean();
        int tam = arquivo.readInt();

        if (!ativo)
            return false;

        // marca antigo como removido
        arquivo.seek(pos);
        arquivo.writeBoolean(false);

        // grava novo no final
        long novaPos = arquivo.length();
        arquivo.seek(novaPos);

        byte[] novos = novo.toByteArray();

        arquivo.writeBoolean(true);
        arquivo.writeInt(novos.length);
        arquivo.write(novos);

        // atualiza hash
        hash.remover(novo.getId());
        hash.inserir(novo.getId(), novaPos);

        return true;
    }

    // =========================
    // DELETE (COM HASH 🚀)
    // =========================
    public boolean delete(int id) throws IOException {
        long pos = hash.buscar(id);

        if (pos == -1)
            return false;

        arquivo.seek(pos);

        boolean ativo = arquivo.readBoolean();

        if (!ativo)
            return false;

        arquivo.seek(pos);
        arquivo.writeBoolean(false);

        hash.remover(id);

        return true;
    }

    // =========================
    // RECONSTRUIR HASH
    // =========================
    public void reconstruirHash() throws IOException {
        RandomAccessFile dir =
                new RandomAccessFile("data/diretorios/autores_dir.hash", "rw");
        dir.setLength(0);
        dir.close();

        RandomAccessFile bucket =
                new RandomAccessFile("data/buckets/autores_bucket.hash", "rw");
        bucket.setLength(0);
        bucket.close();

        hash = new HashExtensivel("autores");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long pos = arquivo.getFilePointer();

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Autor a = new Autor();
                a.fromByteArray(dados);

                hash.inserir(a.getId(), pos);
            }
        }

        System.out.println("Hash reconstruído!");
    }
}