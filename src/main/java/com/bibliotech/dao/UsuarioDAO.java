package com.bibliotech.dao;

import com.bibliotech.model.Usuario;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UsuarioDAO {

    private final RandomAccessFile arquivo;
    private HashExtensivel hash;
    private final String CHAVE_XOR = "pucminas";

    private final Object lock = new Object();

    public UsuarioDAO() throws IOException {

        new File("data").mkdirs();
        new File("data/diretorios").mkdirs();
        new File("data/buckets").mkdirs();

        arquivo = new RandomAccessFile("data/usuarios.dat", "rw");

        if (arquivo.length() == 0) {
            arquivo.writeInt(0);
            arquivo.writeInt(0);
        }

        hash = new HashExtensivel("usuarios");

        try {
            reconstruirHash();
        } catch (Exception e) {
            System.out.println("[UsuarioDAO] .dat corrompido — resetando...");
            resetarDat();
            reconstruirHash();
        }

        if (datVazio()) {
            Usuario admin = Usuario.criarAdminPadrao();
            create(admin);
            System.out.println("[UsuarioDAO] Admin padrão criado.");
        }
    }


    // CRIPTOGRAFIA XOR + Base64
    // XOR puro pode gerar bytes nulos ou de controle que corrompem o writeUTF.
    // Encapsular em Base64 garante que a string gravada é sempre ASCII segura.

    private String criptografar(String senha) {
        byte[] bytes = senha.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] chave = CHAVE_XOR.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++)
            bytes[i] ^= chave[i % chave.length];
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String descriptografar(String senhaCriptografada) {
        byte[] bytes = Base64.getDecoder().decode(senhaCriptografada);
        byte[] chave = CHAVE_XOR.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++)
            bytes[i] ^= chave[i % chave.length];
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

 
    // HELPER — lê um registro de forma segura; retorna null se truncado
    private byte[] lerRegistroSeguro(int tamanho) {
        try {
            long restante = arquivo.length() - arquivo.getFilePointer();
            if (tamanho < 0 || tamanho > restante) {
                System.out.println("[UsuarioDAO] Registro truncado detectado em pos "
                    + arquivo.getFilePointer() + " (tamanho=" + tamanho
                    + ", restante=" + restante + "). Ignorando restante do arquivo.");
                return null;
            }
            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);
            return dados;
        } catch (IOException e) {
            return null;
        }
    }


    // CREATE
    public int create(Usuario usuario) throws IOException {
        synchronized (lock) {
            arquivo.seek(0);
            int ultimoID   = arquivo.readInt();
            int quantidade = arquivo.readInt();

            ultimoID++;
            quantidade++;
            usuario.setId(ultimoID);

            if (hash.buscar(usuario.getId()) != -1)
                throw new IllegalArgumentException(
                    "PK duplicada: já existe usuário com id=" + usuario.getId());

            String senhaOriginal = usuario.getSenha();
            usuario.setSenha(criptografar(senhaOriginal));

            long   pos   = arquivo.length();
            byte[] dados = usuario.toByteArray();

            arquivo.seek(pos);
            arquivo.writeBoolean(true);
            arquivo.writeInt(dados.length);
            arquivo.write(dados);

            hash.inserir(usuario.getId(), pos);

            arquivo.seek(0);
            arquivo.writeInt(ultimoID);
            arquivo.writeInt(quantidade);

            usuario.setSenha(senhaOriginal);
            return usuario.getId();
        }
    }

  
    // READ
    public Usuario read(int id) throws IOException {
        synchronized (lock) {
            long pos = hash.buscar(id);
            if (pos == -1) return null;

            arquivo.seek(pos);
            boolean ativo   = arquivo.readBoolean();
            int     tamanho = arquivo.readInt();
            if (!ativo) return null;

            byte[] dados = lerRegistroSeguro(tamanho);
            if (dados == null) return null;

            Usuario u = new Usuario();
            u.fromByteArray(dados);
            u.setSenha(descriptografar(u.getSenha()));
            return u;
        }
    }

    // LIST ALL
    public List<Usuario> listAll() throws IOException {
        synchronized (lock) {
            List<Usuario> usuarios = new ArrayList<>();
            arquivo.seek(8);

            while (arquivo.getFilePointer() < arquivo.length()) {
                long restante = arquivo.length() - arquivo.getFilePointer();
                if (restante < 5) {
                    System.out.println("[UsuarioDAO] listAll: cabeçalho truncado no fim do arquivo ("
                        + restante + " bytes). Interrompendo leitura.");
                    break;
                }

                boolean ativo   = arquivo.readBoolean();
                int     tamanho = arquivo.readInt();

                if (ativo) {
                    byte[] dados = lerRegistroSeguro(tamanho);
                    if (dados == null) break;

                    try {
                        Usuario u = new Usuario();
                        u.fromByteArray(dados);
                        u.setSenha(descriptografar(u.getSenha()));
                        usuarios.add(u);
                    } catch (Exception e) {
                        System.out.println("[UsuarioDAO] listAll: erro ao desserializar registro. Ignorando.");
                    }
                } else {
                    long restanteParaPular = arquivo.length() - arquivo.getFilePointer();
                    if (tamanho < 0 || tamanho > restanteParaPular) {
                        System.out.println("[UsuarioDAO] listAll: skipBytes inválido ("
                            + tamanho + "). Interrompendo leitura.");
                        break;
                    }
                    arquivo.skipBytes(tamanho);
                }
            }
            return usuarios;
        }
    }


    // UPDATE
    public boolean update(Usuario novoUsuario) throws IOException {
        synchronized (lock) {

            long pos = hash.buscar(novoUsuario.getId());

            if (pos == -1)
                return false;

            arquivo.seek(pos);

            boolean ativo = arquivo.readBoolean();
            int tamanhoAntigo = arquivo.readInt();

            if (!ativo)
                return false;

            String senhaOriginal = novoUsuario.getSenha();
            novoUsuario.setSenha(criptografar(senhaOriginal));

            byte[] novosDados = novoUsuario.toByteArray();

        
            // cabe no espaço antigo
            if (novosDados.length <= tamanhoAntigo) {

                // mantém tamanho antigo para evitar lixo/truncamento
                arquivo.seek(pos + 5); // começa após lapide + tamanho
                arquivo.write(novosDados);

                // preenche sobra com zeros
                for(int i=novosDados.length; i<tamanhoAntigo; i++){
                    arquivo.writeByte(0);
                }

            }
            else {

                // exclusão lógica do antigo
                arquivo.seek(pos);
                arquivo.writeBoolean(false);

                long novaPos = arquivo.length();

                arquivo.seek(novaPos);

                arquivo.writeBoolean(true);
                arquivo.writeInt(novosDados.length);
                arquivo.write(novosDados);

                hash.remover(novoUsuario.getId());
                hash.inserir(novoUsuario.getId(), novaPos);
            }

            novoUsuario.setSenha(senhaOriginal);

            return true;
        }
    }


    // DELETE
    public boolean delete(int id) throws IOException {
        synchronized (lock) {
            long pos = hash.buscar(id);
            if (pos == -1) return false;

            arquivo.seek(pos);
            boolean ativo = arquivo.readBoolean();
            if (!ativo) return false;

            arquivo.seek(pos);
            arquivo.writeBoolean(false);
            hash.remover(id);
            return true;
        }
    }


    // LOGIN
    public Usuario login(String email, String senha) throws IOException {
        synchronized (lock) {
            String senhaCriptografada = criptografar(senha);

            arquivo.seek(8);
            while (arquivo.getFilePointer() < arquivo.length()) {
                long restante = arquivo.length() - arquivo.getFilePointer();
                if (restante < 5) break;

                boolean ativo   = arquivo.readBoolean();
                int     tamanho = arquivo.readInt();

                byte[] dados = lerRegistroSeguro(tamanho);
                if (dados == null) break;

                if (ativo) {
                    try {
                        Usuario u = new Usuario();
                        u.fromByteArray(dados);

                        if (u.getEmail().equals(email) &&
                            u.getSenha().equals(senhaCriptografada)) {
                            u.setSenha(senha);
                            return u;
                        }
                    } catch (Exception e) {
                        System.out.println("[UsuarioDAO] login: erro ao desserializar registro. Ignorando.");
                    }
                }
            }
            return null;
        }
    }

  
    // LISTAR NO TERMINAL
    public void listar() throws IOException {
        System.out.println("\n--- LISTA DE USUÁRIOS ---");
        List<Usuario> usuarios = listAll();
        if (usuarios.isEmpty()) {
            System.out.println("Nenhum usuário cadastrado.");
        } else {
            for (Usuario u : usuarios)
                System.out.println("ID: " + u.getId()
                    + " | Nome: "  + u.getNome()
                    + " | Email: " + u.getEmail()
                    + " | Tipo: "  + u.getTipo());
        }
        System.out.println("-------------------------\n");
    }

 
    // RECONSTRUIR HASH
    public void reconstruirHash() throws IOException {
        zerarArquivo("data/diretorios/usuarios_dir.hash");
        zerarArquivo("data/buckets/usuarios_bucket.hash");

        hash = new HashExtensivel("usuarios");

        arquivo.seek(8);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long restante = arquivo.length() - arquivo.getFilePointer();
            if (restante < 5) break;

            long    pos   = arquivo.getFilePointer();
            boolean ativo = arquivo.readBoolean();
            int     tam   = arquivo.readInt();

            byte[] dados = lerRegistroSeguro(tam);
            if (dados == null) break;

            if (ativo) {
                try {
                    Usuario u = new Usuario();
                    u.fromByteArray(dados);
                    hash.inserir(u.getId(), pos);
                } catch (Exception e) {
                    System.out.println("[UsuarioDAO] reconstruirHash: registro inválido ignorado.");
                }
            }
        }
        System.out.println("[UsuarioDAO] Hash reconstruído com sucesso.");
    }


    // HELPERS PRIVADOS
    private boolean datVazio() throws IOException {
        arquivo.seek(8);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long restante = arquivo.length() - arquivo.getFilePointer();
            if (restante < 5) break;

            boolean ativo = arquivo.readBoolean();
            int     tam   = arquivo.readInt();
            if (ativo) return false;

            long restanteParaPular = arquivo.length() - arquivo.getFilePointer();
            if (tam < 0 || tam > restanteParaPular) break;
            arquivo.skipBytes(tam);
        }
        return true;
    }

    private void resetarDat() throws IOException {
        arquivo.setLength(0);
        arquivo.seek(0);
        arquivo.writeInt(0);
        arquivo.writeInt(0);
        zerarArquivo("data/diretorios/usuarios_dir.hash");
        zerarArquivo("data/buckets/usuarios_bucket.hash");
        hash = new HashExtensivel("usuarios");
        System.out.println("[UsuarioDAO] Arquivos resetados com sucesso.");
    }

    private void zerarArquivo(String caminho) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(caminho, "rw")) {
            f.setLength(0);
        }
    }

    public void fechar() throws IOException {
        arquivo.close();
        hash.fechar();
    }
}