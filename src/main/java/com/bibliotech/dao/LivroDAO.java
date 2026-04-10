package com.bibliotech.dao;

import com.bibliotech.model.Livro;

import java.io.*;

public class LivroDAO {

    private RandomAccessFile arquivo;

    public LivroDAO() throws IOException {
        arquivo = new RandomAccessFile("data/livros.dat", "rw");

        // Se arquivo vazio, cria cabeçalho
        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // último ID
            arquivo.writeInt(0); // quantidade de registros
        }
    }

    // =========================
    // CREATE
    // =========================
    public int create(Livro livro) throws IOException {

        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int quantidade = arquivo.readInt();

        ultimoID++;
        quantidade++;

        livro.setId(ultimoID);

        arquivo.seek(arquivo.length());

        byte[] dados = livro.toByteArray();

        arquivo.writeBoolean(true); // ativo
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        // atualiza cabeçalho
        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);

        return livro.getId();
    }

    // =========================
    // READ
    // =========================
    public Livro read(int id) throws IOException {

        arquivo.seek(8); // pula cabeçalho

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                // DEBUG (opcional)
                // System.out.println("Lido ID: " + l.getId());

                if (l.getId() == id) {
                    return l;
                }
            }
        }

        return null;
    }

    // =========================
    // DELETE (lápide)
    // =========================
    public boolean delete(int id) throws IOException {

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long posRegistro = arquivo.getFilePointer();

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                if (l.getId() == id) {
                    arquivo.seek(posRegistro);
                    arquivo.writeBoolean(false); // marca como deletado
                    return true;
                }
            }
        }

        return false;
    }

    // =========================
    // UPDATE
    // =========================
    public boolean update(Livro novoLivro) throws IOException {

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long posRegistro = arquivo.getFilePointer();

            boolean ativo = arquivo.readBoolean();
            int tamanhoAntigo = arquivo.readInt();

            long posDados = arquivo.getFilePointer();

            byte[] dados = new byte[tamanhoAntigo];
            arquivo.readFully(dados);

            Livro l = new Livro();
            l.fromByteArray(dados);

            if (ativo && l.getId() == novoLivro.getId()) {

                byte[] novosDados = novoLivro.toByteArray();

                if (novosDados.length <= tamanhoAntigo) {
                    // sobrescreve no mesmo espaço
                    arquivo.seek(posDados);
                    arquivo.write(novosDados);
                } else {
                    // marca antigo como inativo
                    arquivo.seek(posRegistro);
                    arquivo.writeBoolean(false);

                    // escreve novo no final
                    arquivo.seek(arquivo.length());
                    arquivo.writeBoolean(true);
                    arquivo.writeInt(novosDados.length);
                    arquivo.write(novosDados);
                }

                return true;
            }
        }

        return false;
    }

    // =========================
    // LISTAR
    // =========================
    public void listar() throws IOException {

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);
                System.out.println(l);
            }
        }
    }
}