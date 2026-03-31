package com.bibliotech.dao;

import com.bibliotech.model.Emprestimo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class EmprestimoDAO {

    private RandomAccessFile arquivo;

    public EmprestimoDAO() throws IOException {
        arquivo = new RandomAccessFile("data/emprestimos.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0); // ultimoId
            arquivo.writeInt(0); // quantidadeRegistros
        }
    }

    public int create(Emprestimo emprestimo) throws IOException {
        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int quantidade = arquivo.readInt();

        ultimoID++;
        quantidade++;
        emprestimo.setId(ultimoID);

        arquivo.seek(arquivo.length());
        byte[] dados = emprestimo.toByteArray();

        arquivo.writeBoolean(true);      // lápide ativa
        arquivo.writeInt(dados.length);
        arquivo.write(dados);

        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);

        return emprestimo.getId();
    }

    public Emprestimo read(int id) throws IOException {
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Emprestimo e = new Emprestimo();
                e.fromByteArray(dados);
                if (e.getId() == id) return e;
            }
        }
        return null;
    }

    public List<Emprestimo> listAll() throws IOException {
        List<Emprestimo> lista = new ArrayList<>();
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            if (ativo) {
                byte[] dados = new byte[tamanho];
                arquivo.readFully(dados);
                Emprestimo e = new Emprestimo();
                e.fromByteArray(dados);
                e.atualizarStatus(); // atualiza status dinâmico (atraso)
                lista.add(e);
            } else {
                arquivo.skipBytes(tamanho);
            }
        }
        return lista;
    }

    public boolean delete(int id) throws IOException {
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long enderecoRegistro = arquivo.getFilePointer();
            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            if (ativo) {
                int idAtual = arquivo.readInt();
                if (idAtual == id) {
                    arquivo.seek(enderecoRegistro);
                    arquivo.writeBoolean(false);
                    return true;
                }
                arquivo.skipBytes(tamanho - 4);
            } else {
                arquivo.skipBytes(tamanho);
            }
        }
        return false;
    }

    public boolean update(Emprestimo novoEmprestimo) throws IOException {
        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {
            long enderecoRegistro = arquivo.getFilePointer();
            boolean ativo = arquivo.readBoolean();
            int tamanhoAntigo = arquivo.readInt();
            long enderecoDados = arquivo.getFilePointer();

            int idAtual = arquivo.readInt();

            if (ativo && idAtual == novoEmprestimo.getId()) {
                byte[] novosDados = novoEmprestimo.toByteArray();

                if (novosDados.length <= tamanhoAntigo) {
                    arquivo.seek(enderecoDados);
                    arquivo.write(novosDados);
                } else {
                    arquivo.seek(enderecoRegistro);
                    arquivo.writeBoolean(false);
                    arquivo.seek(arquivo.length());
                    arquivo.writeBoolean(true);
                    arquivo.writeInt(novosDados.length);
                    arquivo.write(novosDados);
                }
                return true;
            }
            arquivo.skipBytes(tamanhoAntigo - 4);
        }
        return false;
    }

    /**
     * Registra a devolução de um empréstimo pelo ID.
     */
    public boolean registrarDevolucao(int id) throws IOException {
        Emprestimo e = read(id);
        if (e == null) return false;

        e.setStatus("Devolvido");
        return update(e);
    }
}