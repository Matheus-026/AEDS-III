package com.bibliotech.controller;

import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Permite que o front-end chame esta API
public class UsuarioController {

    private UsuarioDAO usuarioDAO;

    public UsuarioController() {
        try {
            this.usuarioDAO = new UsuarioDAO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // FLUXO DE AUTENTICAÇÃO 
    // ==========================================

    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrar(@RequestBody Usuario novoUsuario) {
        try {
            novoUsuario.setTipo("Standard"); 
            int id = usuarioDAO.create(novoUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body("Usuário criado com o ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao criar usuário.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody LoginRequest request) {
        try {
            Usuario u = usuarioDAO.login(request.getEmail(), request.getSenha());
            if (u != null) {
                return ResponseEntity.ok(u);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ==========================================
    // FLUXO ADMINISTRATIVO
    // ==========================================

    @GetMapping
    public List<Usuario> listar() throws IOException {
        return usuarioDAO.listAll();
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable int id) throws IOException {
        Usuario u = usuarioDAO.read(id);
        if (u == null) {
            throw new RuntimeException("Usuário não encontrado");
        }
        return u;
    }

    @PostMapping
    public Map<String, Object> criar(@RequestBody Map<String, String> dados) throws Exception {
        String nome = dados.get("nome");
        String email = dados.get("email");
        String senha = dados.get("senha");
        String tipo = dados.getOrDefault("tipo", "Standard");

        Usuario novoUsuario = new Usuario(nome, email, senha, tipo);
        int novoId = usuarioDAO.create(novoUsuario);

        return Map.of("id", novoId);
    }

    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
            @PathVariable int id,
            @RequestBody Map<String, String> dados) throws Exception {

        Usuario existente = usuarioDAO.read(id);
        if (existente == null) {
            throw new Exception("Usuário não encontrado");
        }

        if (dados.containsKey("nome")) existente.setNome(dados.get("nome"));
        if (dados.containsKey("email")) existente.setEmail(dados.get("email"));
        if (dados.containsKey("senha")) existente.setSenha(dados.get("senha"));
        if (dados.containsKey("tipo")) existente.setTipo(dados.get("tipo"));

        usuarioDAO.update(existente);
        return Map.of("ok", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletar(@PathVariable int id) throws IOException {
        boolean ok = usuarioDAO.delete(id);
        if (!ok) {
            throw new RuntimeException("Usuário não encontrado");
        }
        return Map.of("ok", true);
    }
}

// Classe auxiliar para receber os dados do JSON no momento do login
class LoginRequest {
    private String email;
    private String senha;
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}