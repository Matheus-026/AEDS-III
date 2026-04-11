package com.bibliotech.controller;

import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Usuario;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*") // Permite que o teu front-end chame esta API
public class UsuarioController {

    private UsuarioDAO usuarioDAO;

    public UsuarioController() {
        try {
            this.usuarioDAO = new UsuarioDAO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Rota para o CADASTRO
    @PostMapping("/cadastrar")
    public ResponseEntity<String> cadastrar(@RequestBody Usuario novoUsuario) {
        try {
            // Define como utilizador comum por defeito
            novoUsuario.setTipo("Standard"); 
            int id = usuarioDAO.create(novoUsuario);
            return ResponseEntity.status(HttpStatus.CREATED).body("Utilizador criado com o ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao criar utilizador.");
        }
    }

    // Rota para o LOGIN
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody LoginRequest request) {
        try {
            Usuario u = usuarioDAO.login(request.getEmail(), request.getSenha());
            if (u != null) {
                return ResponseEntity.ok(u); // Retorna os dados do utilizador (sucesso)
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Erro 401 (falha)
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// Classe auxiliar apenas para receber os dados do JSON do Front-end no momento do login
class LoginRequest {
    private String email;
    private String senha;
    
    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}