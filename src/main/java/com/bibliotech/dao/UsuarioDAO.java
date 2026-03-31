package com.bibliotech.dao;
import com.bibliotech.model.Usuario;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
public class UsuarioDAO {

    private RandomAccessFile arquivo;
    private final String CHAVE_XOR = "pucminas"; // Chave para criptografia

    public UsuarioDAO() throws IOException{

        arquivo = new RandomAccessFile("data/usuarios.dat", "rw");

        // Se não possuir metadados, cabeçalho vazio
        if(arquivo.length() == 0){
            // Inicializa cabeçalho (8 bytes) [cite: 57]
            arquivo.writeInt(0); //UltimoId
			arquivo.writeInt(0); // quantidadeRegistros

            // Cria o primeiro usuário ADM automaticamente
            Usuario adminInicial = Usuario.criarAdminPadrao();
            this.create(adminInicial);
            // Note: o create já incrementa o cabeçalho para 1
        }
    }

    // Método auxiliar para criptografia XOR 
    private String aplicarXOR(String senha) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < senha.length(); i++) {
            sb.append((char) (senha.charAt(i) ^ CHAVE_XOR.charAt(i % CHAVE_XOR.length())));
        }
        return sb.toString();
    }

    public int create(Usuario usuario) throws IOException {
        arquivo.seek(0);
        int ultimoID = arquivo.readInt();
        int quantidade = arquivo.readInt();
        
        ultimoID++;
        quantidade++;
        usuario.setId(ultimoID);

        // Aplica criptografia na senha antes de salvar [cite: 52]
        usuario.setSenha(aplicarXOR(usuario.getSenha()));
        
        arquivo.seek(arquivo.length());
        byte[] dados = usuario.toByteArray();
        
        arquivo.writeBoolean(true);  // Lápide ativa [cite: 57]
        arquivo.writeInt(dados.length);
        arquivo.write(dados);
        
        // Atualiza cabeçalho [cite: 57]
        arquivo.seek(0);
        arquivo.writeInt(ultimoID);
        arquivo.writeInt(quantidade);
        
        return usuario.getId();
    }

    // Retorna uma lista para o Front-end
    public List<Usuario> listAll() throws IOException {
        List<Usuario> usuarios = new ArrayList<>();
        arquivo.seek(8); // Pula cabeçalho

        while (arquivo.getFilePointer() < arquivo.length()) {
            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            if (ativo) {
                byte[] dados = new byte[tamanho];
                arquivo.readFully(dados);
                Usuario u = new Usuario();
                u.fromByteArray(dados);
                usuarios.add(u);
            } else {
                arquivo.skipBytes(tamanho);
            }
        }
        return usuarios;
    }

    // Método para listar os usuários diretamente no terminal (Fase 1)
    public void listar() throws IOException {
        System.out.println("\n--- LISTA DE USUÁRIOS NO ARQUIVO BINÁRIO ---");
        
        // Reutilizamos o listAll para pegar todos os registros ativos
        List<Usuario> usuarios = this.listAll();
        
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
        } else {
            for (Usuario u : usuarios) {
                System.out.println("ID: " + u.getId() + 
                                   " | Nome: " + u.getNome() + 
                                   " | Email: " + u.getEmail() + 
                                   " | Tipo: " + u.getTipo() + 
                                   " | Senha(XOR): " + u.getSenha());
            }
        }
        System.out.println("-------------------------------------------\n");
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

	public boolean update(Usuario novoUsuario) throws IOException {
		arquivo.seek(8);
		
		while (arquivo.getFilePointer() < arquivo.length()) {
			long enderecoRegistro = arquivo.getFilePointer();
			boolean ativo = arquivo.readBoolean();
			int tamanhoAntigo = arquivo.readInt();
			long enderecoDados = arquivo.getFilePointer();
			
			int idAtual = arquivo.readInt();
			
			if (ativo && idAtual == novoUsuario.getId()) {
                // Garante que a nova senha também seja criptografada antes de atualizar
                novoUsuario.setSenha(aplicarXOR(novoUsuario.getSenha()));
                
				byte[] novosDados = novoUsuario.toByteArray();
				
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
	
	public Usuario read(int id) throws IOException {
	    arquivo.seek(8);

	    while (arquivo.getFilePointer() < arquivo.length()) {
	        boolean ativo = arquivo.readBoolean();
	        int tamanho = arquivo.readInt();

	        byte[] dados = new byte[tamanho];
	        arquivo.readFully(dados);

	        if (ativo) {
	            Usuario u = new Usuario();
	            u.fromByteArray(dados);

	            if (u.getId() == id) {
	                return u;
	            }
	        }
	    }
	    return null;
	}
}