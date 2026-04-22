package com.bibliotech.model;

import java.io.*;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String tipo;

    public Usuario() {}

    public static Usuario criarAdminPadrao() {
        return new Usuario("Administrador", "admin@bibliotech.com", "admin@#$", "Adm");
    }

    public Usuario(String nome, String email, String senha, String tipo) {
        this.nome  = nome;
        this.email = email;
        this.senha = senha;
        this.tipo  = (tipo == null || tipo.isEmpty()) ? "Standard" : tipo;
    }

    public void setId(int id)          { this.id = id; }
    public int  getId()                { return id; }

    public void   setNome(String nome) { this.nome = nome; }
    public String getNome()            { return nome; }

    public void   setEmail(String email) { this.email = email; }
    public String getEmail()             { return email; }

    public void   setSenha(String senha) { this.senha = senha; }
    public String getSenha()             { return senha; }

    public void   setTipo(String tipo) { this.tipo = tipo; }
    public String getTipo()            { return tipo; }

    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream ba  = new ByteArrayOutputStream();
        DataOutputStream      dos = new DataOutputStream(ba);

        // Garante que nenhum campo String seja null antes de gravar.
        // Um null causaria NullPointerException no writeUTF e corromperia
        // o arquivo parcialmente escrito.
        dos.writeInt(id);
        dos.writeUTF(nome  != null ? nome  : "");
        dos.writeUTF(email != null ? email : "");
        dos.writeUTF(senha != null ? senha : "");
        dos.writeUTF(tipo  != null ? tipo  : "Standard");

        return ba.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream      dis  = new DataInputStream(bais);

        id    = dis.readInt();
        nome  = dis.readUTF();
        email = dis.readUTF();
        senha = dis.readUTF();
        tipo  = dis.readUTF();
    }
}