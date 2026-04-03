package com.bibliotech.model;

import java.io.*;

public class Autor {

    private int id;
    private String nome;
    private String telefone;
    private String biografia;

    public Autor() {}

    public Autor(String nome, String telefone, String biografia) {
        this.nome = nome;
        this.telefone = telefone;
        this.biografia = biografia;
    }

    // GETS E SETS
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }

    // SERIALIZAÇÃO
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeUTF(nome);
        dos.writeUTF(telefone != null ? telefone : "");
        dos.writeUTF( biografia != null ? biografia : "");

        return ba.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        nome = dis.readUTF();
        telefone = dis.readUTF();
        biografia = dis.readUTF();
    }
}