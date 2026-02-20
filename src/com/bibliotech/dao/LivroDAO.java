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
		
		arquivo.writeBoolean(true); // lapide ativa
		arquivo.writeInt(dados.length);
		arquivo.write(dados);
		
		
		return livro.getId();
		
	}
	
}
