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
	private int idAutor;

	public Livro() {}
	
	public Livro(String titulo, String resumo, float preco, 
            LocalDate dataPublicacao, String[] generos, int idAutor) {
		this.titulo = titulo;
		this.resumo = resumo;
		this.preco = preco;
		this.dataPublicacao = dataPublicacao;
		this.generos = generos;
		this.idAutor = idAutor;
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
	
	public int getIdAutor() { return idAutor; }
	public void setIdAutor(int idAutor) { this.idAutor = idAutor; }


	
	public byte[] toByteArray() throws IOException {

        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(id);
        dos.writeUTF(titulo);
        dos.writeUTF(resumo);
        dos.writeFloat(preco);
        dos.writeUTF(dataPublicacao.toString());
        dos.writeInt(idAutor);

		dos.writeInt(generos.length);  // quantidade de gêneros
		for(String genero : generos){
			dos.writeUTF(genero);
		}
        return ba.toByteArray();
    }

	public void fromByteArray(byte[] ba) throws IOException{ // Ele interpreta os bytes e preenche os campos do objeto
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        titulo = dis.readUTF();
        resumo = dis.readUTF();
        preco = dis.readFloat();
        dataPublicacao = LocalDate.parse(dis.readUTF());
        
        idAutor = dis.readInt();

		int qntGeneros = dis.readInt();
		generos = new String[qntGeneros];

		for(int i = 0; i < qntGeneros; i++){
			generos[i] = dis.readUTF();		
		}
		
		
    }
}
