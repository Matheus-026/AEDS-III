package com.bibliotech.dao;

import com.bibliotech.model.Autor;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AutorDAO {

    private RandomAccessFile arquivo;
    private HashExtensivel hash;

    public AutorDAO() throws IOException {

        new File("data").mkdirs();
        new File("data/diretorios").mkdirs();
        new File("data/buckets").mkdirs();

        arquivo = new RandomAccessFile("data/autores.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // último ID
            arquivo.writeInt(0); // quantidade
        }

        hash = new HashExtensivel("autores");

        if (hashEstaVazio()) {
            reconstruirHash();
        }
    }

    // VERIFICAR HASH
    private boolean hashEstaVazio() {
        File dir    = new File("data/diretorios/autores_dir.hash");
        File bucket = new File("data/buckets/autores_bucket.hash");
        return !dir.exists()    || dir.length()    <= 4
            || !bucket.exists() || bucket.length() == 0;
    }


    // CREATE
    public int create(Autor autor) throws IOException {

        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int qtd      = arquivo.readInt();

        ultimoID++;
        qtd++;
        autor.setId(ultimoID);

        // ── validação: PK duplicada 
        if (hash.buscar(autor.getId()) != -1) {
            throw new IllegalArgumentException(
                "PK duplicada: já existe autor com id=" + autor.getId());
        }

        long pos   = arquivo.length();
        byte[] dados = autor.toByteArray();

        arquivo.seek(pos);
        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // atualiza índice primário
        hash.inserir(autor.getId(), pos);

        // atualiza cabeçalho
        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(qtd);

        return autor.getId();
    }


    // READ
    public Autor read(int id) throws IOException {

        long pos = hash.buscar(id);

        if (pos == -1)
            return null;

        arquivo.seek(pos);
        boolean ativo   = arquivo.readBoolean();
        int     tamanho = arquivo.readInt();

        if (!ativo)
            return null;

        byte[] dados = new byte[tamanho];
        arquivo.readFully(dados);

        Autor a = new Autor();
        a.fromByteArray(dados);
        return a;
    }


    // READ ALL
    public List<Autor> readAll() throws IOException {

        List<Autor> lista = new ArrayList<>();
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo   = arquivo.readBoolean();
            int     tamanho = arquivo.readInt();
            byte[]  dados   = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Autor a = new Autor();
                a.fromByteArray(dados);
                lista.add(a);
            }
        }

        return lista;
    }


    // UPDATE
    public boolean update(Autor novo) throws IOException {

        long pos = hash.buscar(novo.getId());

        if (pos == -1)
            return false;

        arquivo.seek(pos);
        boolean ativo = arquivo.readBoolean();

        if (!ativo)
            return false;

        // marca antigo como removido
        arquivo.seek(pos);
        arquivo.writeBoolean(false);

        // grava novo no final
        long   novaPos = arquivo.length();
        byte[] dados   = novo.toByteArray();

        arquivo.seek(novaPos);
        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // atualiza hash
        hash.remover(novo.getId());
        hash.inserir(novo.getId(), novaPos);

        return true;
    }


    // DELETE
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

    // RECONSTRUIR HASH
    /**
     * Reconstrói o índice primário percorrendo o arquivo .dat.
     * Chamado automaticamente se o índice estiver ausente.
     */
    public void reconstruirHash() throws IOException {

        zerarArquivo("data/diretorios/autores_dir.hash");
        zerarArquivo("data/buckets/autores_bucket.hash");

        hash = new HashExtensivel("autores");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long    pos    = arquivo.getFilePointer();
            boolean ativo  = arquivo.readBoolean();
            int     tam    = arquivo.readInt();
            byte[]  dados  = new byte[tam];
            arquivo.readFully(dados);

            if (ativo) {
                Autor a = new Autor();
                a.fromByteArray(dados);
                hash.inserir(a.getId(), pos);
            }
        }

        System.out.println("[AutorDAO] Hash reconstruído com sucesso.");
    }

   
    // UTILITÁRIOS
    private void zerarArquivo(String caminho) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(caminho, "rw")) {
            f.setLength(0);
        }
    }

    public void fechar() throws IOException {
        arquivo.close();
        hash.fechar();
    }
}