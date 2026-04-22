package com.bibliotech.controller;

import com.bibliotech.dao.EmprestimoDAO;
import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Emprestimo;
import com.bibliotech.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private static UsuarioDAO   usuarioDAO;
    private static EmprestimoDAO emprestimoDAO;

    static {
        try {
            usuarioDAO    = new UsuarioDAO();
            emprestimoDAO = new EmprestimoDAO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    // AUTENTICAÇÃO
    // =========================================================================

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@RequestBody Map<String, String> dados) {
        try {
            String nome  = dados.get("nome");
            String email = dados.get("email");
            String senha = dados.get("senha");

            if (nome == null || email == null || senha == null || senha.isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("nome, email e senha são obrigatórios");

            int id = usuarioDAO.create(new Usuario(nome, email, senha, "Standard"));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao criar usuário: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getSenha() == null)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

            Usuario u = usuarioDAO.login(request.getEmail(), request.getSenha());
            if (u != null) {
                u.setSenha(null); // nunca expõe senha na resposta
                return ResponseEntity.ok(u);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================================================================
    // VERIFICAR SENHA DO ADMINISTRADOR
    // Chamado pelo frontend ANTES de qualquer PUT, para confirmar que quem
    // está editando é realmente um admin autenticado.
    // Recebe: { "senha": "..." }
    // Retorna 200 se algum usuário do tipo "Adm" tiver essa senha, 401 caso contrário.
    // =========================================================================
    @PostMapping("/verificar-admin")
    public ResponseEntity<?> verificarAdmin(@RequestBody Map<String, String> dados) {
        try {
            String senha = dados.get("senha");
            if (senha == null || senha.isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("senha é obrigatória");

            List<Usuario> todos = usuarioDAO.listAll();
            boolean admValido = todos.stream()
                .anyMatch(u -> u.getTipo().equalsIgnoreCase("Adm")
                            && u.getSenha().equals(senha));

            if (admValido)
                return ResponseEntity.ok(Map.of("ok", true));

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Senha do administrador incorreta.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao verificar: " + e.getMessage());
        }
    }

    // =========================================================================
    // CRUD ADMINISTRATIVO
    // =========================================================================

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<Usuario> lista = usuarioDAO.listAll();
            lista.forEach(u -> u.setSenha(null)); // nunca expõe senhas
            return ResponseEntity.ok(lista);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao listar usuários: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            Usuario u = usuarioDAO.read(id);
            if (u == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado");
            u.setSenha(null); // nunca expõe senha
            return ResponseEntity.ok(u);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao buscar usuário: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, String> dados) {
        try {
            String nome  = dados.get("nome");
            String email = dados.get("email");
            String senha = dados.get("senha");
            String tipo  = dados.getOrDefault("tipo", "Standard");

            if (nome == null || email == null || senha == null || senha.isBlank())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("nome, email e senha são obrigatórios");

            int novoId = usuarioDAO.create(new Usuario(nome, email, senha, tipo));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", novoId));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao criar usuário: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable int id,
            @RequestBody Map<String, String> dados) {
        try {
            // read() devolve senha em texto limpo — usamos como base.
            Usuario existente = usuarioDAO.read(id);
            if (existente == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado");

            if (dados.containsKey("nome")  && dados.get("nome")  != null)
                existente.setNome(dados.get("nome"));
            if (dados.containsKey("email") && dados.get("email") != null)
                existente.setEmail(dados.get("email"));
            if (dados.containsKey("tipo")  && dados.get("tipo")  != null)
                existente.setTipo(dados.get("tipo"));

            // "novaSenha" → troca a senha do usuário.
            // Se não vier, a senha limpa do read() é mantida e o update()
            // a criptografa corretamente — sem double XOR.
            String novaSenha = dados.get("novaSenha");
            if (novaSenha != null && !novaSenha.isBlank())
                existente.setSenha(novaSenha);

            boolean ok = usuarioDAO.update(existente);
            if (!ok)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Erro ao atualizar.");

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable int id) {
        try {
            List<Emprestimo> pendencias = emprestimoDAO.buscarPorUsuario(id);
            if (pendencias != null && !pendencias.isEmpty())
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Não é possível excluir: este usuário possui empréstimos ativos.");

            boolean ok = usuarioDAO.delete(id);
            if (!ok)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Usuário não encontrado");

            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro ao deletar: " + e.getMessage());
        }
    }
}

class LoginRequest {
    private String email;
    private String senha;
    public String getEmail()             { return email; }
    public void   setEmail(String email) { this.email = email; }
    public String getSenha()             { return senha; }
    public void   setSenha(String senha) { this.senha = senha; }
}