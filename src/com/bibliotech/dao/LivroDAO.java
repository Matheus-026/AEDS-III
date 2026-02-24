package com.bibliotech.dao;
import com.bibliotech.model.Livro;

import java.io.*;
import java.time.LocalDate;
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
		// 1. Ler os metadados atuais
		arquivo.seek(0);
		int ultimoID = arquivo.readInt();
		int quantidade = arquivo.readInt();
		
		// 2. Incrementar
		ultimoID++;
		quantidade++;
		livro.setId(ultimoID);
		
		// 3. Ir para o fim e escrever o registro PRIMEIRO
		arquivo.seek(arquivo.length());
		byte[] dados = livro.toByteArray();
		
		arquivo.writeBoolean(true); // AGORA SIM: true significa ativo/válido
		arquivo.writeInt(dados.length);
		arquivo.write(dados);
		
		// 4. Atualizar o cabeçalho APÓS o sucesso da escrita
		arquivo.seek(0);
		arquivo.writeInt(ultimoID);
		arquivo.writeInt(quantidade);
		
		return livro.getId();
	}

	public Livro read(int id) throws IOException {
		arquivo.seek(8); // Pula o cabeçalho

		while (arquivo.getFilePointer() < arquivo.length()) {
			boolean ativo = arquivo.readBoolean();
			int tamanho = arquivo.readInt();

			if (ativo) {
				long posicaoAntesId = arquivo.getFilePointer();
				int idAtual = arquivo.readInt();

				if (idAtual == id) {
					// Encontramos! Agora lemos o restante dos campos
					Livro l = new Livro();
					l.setId(idAtual);
					l.setTitulo(arquivo.readUTF());
					l.setResumo(arquivo.readUTF());
					l.setPreco(arquivo.readFloat());
					l.setDataPublicacao(LocalDate.parse(arquivo.readUTF()));
					l.setGeneros(arquivo.readUTF());
					return l;
				} else {
					// Não é este ID, pula o resto deste registro específico
					// (tamanho total - 4 bytes do ID já lido)
					arquivo.skipBytes(tamanho - 4);
				}
			} else {
				// Registro deletado, pula o corpo dele inteirinho
				arquivo.skipBytes(tamanho);
			}
		}
		return null; // Não encontrado
	}

	public boolean delete(int id) throws IOException {
		arquivo.seek(8); // Pula o cabeçalho (2 ints)
		
		while (arquivo.getFilePointer() < arquivo.length()) {
			long enderecoRegistro = arquivo.getFilePointer();
			boolean ativo = arquivo.readBoolean();
			int tamanho = arquivo.readInt();
			
			if (ativo) {
				int idAtual = arquivo.readInt();
				if (idAtual == id) {
					arquivo.seek(enderecoRegistro); // Volta para o início do registro
					arquivo.writeBoolean(false);    // "Lápide": registro agora é inválido
					return true;
				}
				arquivo.skipBytes(tamanho - 4); // Pula o resto do registro (já lemos o ID de 4 bytes)
			} else {
				arquivo.skipBytes(tamanho); // Pula registro já excluído
			}
		}
		return false;
	}

	public boolean update(Livro novoLivro) throws IOException {
		arquivo.seek(8);
		
		while (arquivo.getFilePointer() < arquivo.length()) {
			long enderecoRegistro = arquivo.getFilePointer();
			boolean ativo = arquivo.readBoolean();
			int tamanhoAntigo = arquivo.readInt();
			long enderecoDados = arquivo.getFilePointer();
			
			int idAtual = arquivo.readInt();
			
			if (ativo && idAtual == novoLivro.getId()) {
				byte[] novosDados = novoLivro.toByteArray();
				
				if (novosDados.length <= tamanhoAntigo) {
					// Cabe no mesmo lugar!
					arquivo.seek(enderecoDados);
					arquivo.write(novosDados);
				} else {
					// Não cabe. Exclui o antigo e cria um novo no fim.
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
}
