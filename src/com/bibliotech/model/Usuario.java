package com.bibliotech.model;

import java.io.*;

public class Usuario {
    private int id;
    private String nome;
    private String email;
    private String senha;
    private String tipo; // User ou Adm

    // Contrutor vazio
    public Usuario() {}

    // Método auxiliar para criar o Administrador inicial do sistema
    public static Usuario criarAdminPadrao() {
        return new Usuario("Administrador", "admin@bibliotech.com", "admin@#$", "Adm");
    }

    // Contrutor com os parâmetros completos
    // Construtor padrão (O JS enviará os dados para este)
    public Usuario(String nome, String email, String senha, String tipo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.tipo = (tipo == null || tipo.isEmpty()) ? "Standard" : tipo;
    }

    // Métodos GETS e SETS 
    public void setId(int id) { this.id = id; }
    public int getId() { return id; }

    public void setNome(String nome) { this.nome = nome; }
    public String getNome() { return nome; }

    public void setEmail(String email) { this.email = email; }
    public String getEmail() { return email; }

    public void setSenha(String senha) { this.senha = senha; }
    public String getSenha() { return senha; }

    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTipo() { return tipo; }

    // Escrita em arquivo, com serialização de dados
    public byte[] toByteArray() throws IOException{
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeUTF(email);
        dos.writeUTF(senha);
        dos.writeUTF(tipo);

        return ba.toByteArray();
    }

    // Conversão de bytes, Tradução e desempacotamento
    public void fromByteArray(byte[] ba) throws IOException{
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        nome = dis.readUTF();
        email = dis.readUTF();
        senha = dis.readUTF();
        tipo = dis.readUTF();
    }
}

