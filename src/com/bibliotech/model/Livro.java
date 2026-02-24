package com.bibliotech.model;

import java.io.*;
import java.time.LocalDate;

public class Livro{
	private int id;
	private String titulo;
	private String resumo;
	private float preco;
	private LocalDate dataPublicacao;
	private String[] generos;

	public Livro() {}
	
	public Livro(String titulo, String resumo, float preco, 
            LocalDate dataPublicacao, String[] generos) {
		this.titulo = titulo;
		this.resumo = resumo;
		this.preco = preco;
		this.dataPublicacao = dataPublicacao;
		this.generos = generos;
	}
	
	// Métodos GETS e SETS do livro
	public void setId(int id) { this.id = id; }
	public int getId() { return id; }

	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getTitulo() { return titulo; }

	public void setResumo(String resumo) { this.resumo = resumo; }
	public String getResumo() { return resumo; }

	public void setPreco(float preco) { this.preco = preco; }
	public float getPreco() { return preco; }

	public void setDataPublicacao(LocalDate dataPublicacao) { this.dataPublicacao = dataPublicacao; }
	public LocalDate getDataPublicacao() { return dataPublicacao;}

	public void setGeneros(String[] generos) { this.generos = generos; }
	public String[] getGeneros() { return generos; }


	
	public byte[] toByteArray() throws IOException {

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeUTF(titulo);
        dos.writeUTF(resumo);
        dos.writeFloat(preco);
        dos.writeUTF(dataPublicacao.toString());

		dos.writeInt(generos.length);  // quantidade de gêneros
		for(String genero : generos){
			dos.writeUTF(genero);
		}
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

		int qntGeneros = dis.readInt();
		this.generos = new String[qntGeneros];

		for(int i = 0; i <qntGeneros; i++){
			generos[i] = dis.readUTF();		
		}
    }
}
