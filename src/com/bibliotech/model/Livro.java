package com.bibliotech.model;

import java.io.*;
import java.time.LocalDate;

public class Livro{
	private int id;
	private String titulo;
	private String resumo;
	private float preco;
	private LocalDate dataPublicacao;
	private String generos;
	
	public Livro() {}
	
	public Livro(String titulo, String resumo, float preco, 
            LocalDate dataPublicacao, String generos) {
		this.titulo = titulo;
		this.resumo = resumo;
		this.preco = preco;
		this.dataPublicacao = dataPublicacao;
		this.generos = generos;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public byte[] toByteArray() throws IOException {

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeUTF(titulo);
        dos.writeUTF(resumo);
        dos.writeFloat(preco);
        dos.writeUTF(dataPublicacao.toString());
        dos.writeUTF(generos);

        return ba.toByteArray();
    }

	public void fromByteArray(byte[] ba) throws IOException{ // Ele interpreta os bytes e preenche os campos do objeto
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.titulo = dis.readUTF();
        this.resumo = dis.readUTF();
        this.preco = dis.readFloat();
        this.dataPublicacao = LocalDate.parse(dis.readUTF());
        this.generos = dis.readUTF();
    }
}