package com.bibliotech.dao;
import com.bibliotech.model.Livro;

import java.io.*;
public class LivroDAO{
	
	private RandomAccessFile arquivo;
	
	public LivroDAO() throws IOException{
		
		arquivo = new RandomAccessFile("livros.dat", "rw");
		
		//Se tiver vazio para criar cabeçalho
		
		if(arquivo.length() == 0) {
			arquivo.writeInt(0); //UltimoId
			arquivo.writeInt(0); // quantidadeRegistros
		}
		
	}
	
	public int create(Livro livro) throws IOException {
		
		arquivo.seek(0); // ler o ultimo ID
		int ultimoID = arquivo.readInt();
		int quantidade = arquivo.readInt();
		ultimoID++;
		quantidade++;
		
		livro.setId(ultimoID); // Atualizar ID 
		
		arquivo.seek(0); //Atualizar cabeçalho
		arquivo.writeInt(ultimoID);
		arquivo.writeInt(quantidade);
		
		arquivo.seek(arquivo.length()); //Ir para o fim do arquivo
		
		byte[] dados = livro.toByteArray();
		
		arquivo.writeBoolean(false); // lapide ativa
		arquivo.writeInt(dados.length);
		arquivo.write(dados);
		
		
		return livro.getId();
		
	}
	
	public Livro read(int id) throws IOException {
		arquivo.seek(8);
		while (arquivo.getFilePointer() < arquivo.length()) {
			boolean lapide = arquivo.readBoolean();
			int tamanho = arquivo.readInt();

			byte[] dados = new byte[tamanho];
			arquivo.readFully(dados);

			if(!lapide){
				Livro livro = new Livro();
				livro.fromByteArray(dados);

				if(livro.getId() == id) {
					return livro;
				}
			}
		}
		return null;
	}
}
