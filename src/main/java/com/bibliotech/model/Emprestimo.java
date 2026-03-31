package com.bibliotech.model;

import java.io.*;
import java.time.LocalDate;

public class Emprestimo {
    private int id;
    private int idUsuario;
    private int idLivro;
    private LocalDate dataEmprestimo;
    private LocalDate dataDevolucao;
    private String status; // "Em aberto", "Devolvido", "Atrasado"

    public Emprestimo() {}

    public Emprestimo(int idUsuario, int idLivro, LocalDate dataEmprestimo, LocalDate dataDevolucao) {
        this.idUsuario = idUsuario;
        this.idLivro = idLivro;
        this.dataEmprestimo = dataEmprestimo;
        this.dataDevolucao = dataDevolucao;
        this.status = "Em aberto";
    }

    // Getters e Setters
    public void setId(int id) { this.id = id; }
    public int getId() { return id; }

    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public int getIdUsuario() { return idUsuario; }

    public void setIdLivro(int idLivro) { this.idLivro = idLivro; }
    public int getIdLivro() { return idLivro; }

    public void setDataEmprestimo(LocalDate dataEmprestimo) { this.dataEmprestimo = dataEmprestimo; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }

    public void setDataDevolucao(LocalDate dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    /**
     * Atualiza o status automaticamente comparando dataDevolucao com hoje.
     * Só altera se o empréstimo ainda estiver "Em aberto".
     */
    public void atualizarStatus() {
        if ("Devolvido".equals(this.status)) return;
        if (dataDevolucao != null && LocalDate.now().isAfter(dataDevolucao)) {
            this.status = "Atrasado";
        } else {
            this.status = "Em aberto";
        }
    }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeInt(idUsuario);
        dos.writeInt(idLivro);
        dos.writeUTF(dataEmprestimo.toString());
        dos.writeUTF(dataDevolucao.toString());
        dos.writeUTF(status);

        return ba.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        idUsuario = dis.readInt();
        idLivro = dis.readInt();
        dataEmprestimo = LocalDate.parse(dis.readUTF());
        dataDevolucao = LocalDate.parse(dis.readUTF());
        status = dis.readUTF();
    }
}