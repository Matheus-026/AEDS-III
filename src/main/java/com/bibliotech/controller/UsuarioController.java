package com.bibliotech.controller;

import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Usuario;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin
public class UsuarioController {
    private UsuarioDAO usuarioDAO;

    public UsuarioController() throws IOException {
        usuarioDAO = new UsuarioDAO();
    }

    // 🔹 LISTAR TODOS
    @GetMapping
    public List<Usuario> listar() throws IOException {
        // Retorna a lista de objetos diretamente, o Spring converte para JSON
        return usuarioDAO.listAll();
    }

    // 🔹 BUSCAR POR ID
    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable int id) throws IOException {
        Usuario u = usuarioDAO.read(id);
        if (u == null) {
            throw new RuntimeException("Usuário não encontrado");
        }
        return u;
    }

    // 🔹 CRIAR
    @PostMapping
    public Map<String, Object> criar(@RequestBody Map<String, String> dados) throws Exception {
        // Extrai os campos do JSON enviado pelo Front-end
        String nome = dados.get("nome");
        String email = dados.get("email");
        String senha = dados.get("senha");
        String tipo = dados.getOrDefault("tipo", "Standard");

        Usuario novoUsuario = new Usuario(nome, email, senha, tipo);
        int novoId = usuarioDAO.create(novoUsuario);

        return Map.of("id", novoId);
    }

    // 🔹 ATUALIZAR
    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
            @PathVariable int id,
            @RequestBody Map<String, String> dados) throws Exception {

        Usuario existente = usuarioDAO.read(id);
        if (existente == null) {
            throw new Exception("Usuário não encontrado");
        }

        // Atualiza apenas os campos permitidos
        if (dados.containsKey("nome")) existente.setNome(dados.get("nome"));
        if (dados.containsKey("email")) existente.setEmail(dados.get("email"));
        if (dados.containsKey("senha")) existente.setSenha(dados.get("senha"));
        if (dados.containsKey("tipo")) existente.setTipo(dados.get("tipo"));

        usuarioDAO.update(existente);
        return Map.of("ok", true);
    }

    // 🔹 DELETE
    @DeleteMapping("/{id}")
    public Map<String, Object> deletar(@PathVariable int id) throws IOException {
        boolean ok = usuarioDAO.delete(id);
        if (!ok) {
            throw new RuntimeException("Usuário não encontrado");
        }
        return Map.of("ok", true);
    }
}