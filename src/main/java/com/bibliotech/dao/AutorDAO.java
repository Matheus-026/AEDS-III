package com.bibliotech.dao;

import com.bibliotech.model.Autor;

import java.io.*;

public class AutorDAO {

    private RandomAccessFile arquivo;

    public AutorDAO() throws IOException {
        arquivo = new RandomAccessFile("data/autores.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // ultimo ID
            arquivo.writeInt(0); // quantidade
        }
    }

    public int create(Autor autor) throws IOException {

        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int qtd = arquivo.readInt();

        ultimoID++;
        qtd++;

        autor.setId(ultimoID);

        arquivo.seek(arquivo.length());

        byte[] dados = autor.toByteArray();

        arquivo.writeBoolean(true);
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(qtd);

        return autor.getId();
    }

    public Autor read(int id) throws IOException {

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Autor a = new Autor();
                a.fromByteArray(dados);

                if (a.getId() == id) return a;
            }
        }

        return null;
    }

    public boolean update(Autor novo) throws IOException {

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            long pos = arquivo.getFilePointer();

            boolean ativo = arquivo.readBoolean();
            int tam = arquivo.readInt();

            byte[] dados = new byte[tam];
            arquivo.readFully(dados);

            Autor a = new Autor();
            a.fromByteArray(dados);

            if (ativo && a.getId() == novo.getId()) {

                byte[] novos = novo.toByteArray();

                if (novos.length <= tam) {
                    arquivo.seek(pos + 5);
                    arquivo.write(novos);
                } else {
                    arquivo.seek(pos);
                    arquivo.writeBoolean(false);

                    arquivo.seek(arquivo.length());
                    arquivo.writeBoolean(true);
                    arquivo.writeInt(novos.length);
                    arquivo.write(novos);
                }

                return true;
            }
        }

        return false;
    }

    public boolean delete(int id) throws IOException {

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

                if (a.getId() == id) {
                    arquivo.seek(pos);
                    arquivo.writeBoolean(false);
                    return true;
                }
            }
        }

        return false;
    }
}