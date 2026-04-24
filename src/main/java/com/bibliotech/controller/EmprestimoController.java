package com.bibliotech.controller;

import com.bibliotech.dao.EmprestimoDAO;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Emprestimo;
import com.bibliotech.model.Livro;
import com.bibliotech.model.Usuario;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin
public class EmprestimoController {

    private final EmprestimoDAO emprestimoDAO;
    private final UsuarioDAO usuarioDAO;
    private final LivroDAO livroDAO;

    public EmprestimoController() throws IOException {
        emprestimoDAO = new EmprestimoDAO();
        usuarioDAO    = new UsuarioDAO();
        livroDAO      = new LivroDAO();
    }

    // ─── Tratamento global de exceções ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleErro(Exception ex) {
        ex.printStackTrace();
        String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return ResponseEntity.status(500).body(Map.of("erro", msg));
    }

    // ─── LISTAR TODOS ─────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<Emprestimo> todos = emprestimoDAO.listAll();

            if (todos == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> resposta = new ArrayList<>();

            for (Emprestimo e : todos) {
                try {
                    Usuario u = usuarioDAO.read(e.getIdUsuario());
                    Livro   l = livroDAO.read(e.getIdLivro());

                    Map<String, Object> obj = new HashMap<>();
                    obj.put("id",             e.getId());
                    obj.put("idUsuario",      e.getIdUsuario());
                    obj.put("idLivro",        e.getIdLivro());
                    obj.put("nomeUsuario",    u != null ? u.getNome()   : "Desconhecido");
                    obj.put("tituloLivro",    l != null ? l.getTitulo() : "Desconhecido");
                    obj.put("dataEmprestimo", e.getDataEmprestimo() != null ? e.getDataEmprestimo().toString() : "");
                    obj.put("dataDevolucao",  e.getDataDevolucao()  != null ? e.getDataDevolucao().toString()  : "");
                    obj.put("status",         e.getStatus() != null ? e.getStatus() : "");
                    resposta.add(obj);

                } catch (Exception ex) {
                    // um empréstimo com dados ruins não derruba a lista inteira
                    System.err.println("[listar] Empréstimo id=" + e.getId() + " ignorado: " + ex.getMessage());
                }
            }

            return ResponseEntity.ok(resposta);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── BUSCAR POR ID ────────────────────────────────────────────────────────
    @GetMapping(params = "id")
    public ResponseEntity<?> buscarPorId(@RequestParam int id) {
        try {
            Emprestimo e = emprestimoDAO.read(id);

            if (e == null) {
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            }

            Usuario u = usuarioDAO.read(e.getIdUsuario());
            Livro   l = livroDAO.read(e.getIdLivro());

            Map<String, Object> obj = new HashMap<>();
            obj.put("id",             e.getId());
            obj.put("idUsuario",      e.getIdUsuario());
            obj.put("idLivro",        e.getIdLivro());
            obj.put("nomeUsuario",    u != null ? u.getNome()   : "Desconhecido");
            obj.put("tituloLivro",    l != null ? l.getTitulo() : "Desconhecido");
            obj.put("dataEmprestimo", e.getDataEmprestimo() != null ? e.getDataEmprestimo().toString() : "");
            obj.put("dataDevolucao",  e.getDataDevolucao()  != null ? e.getDataDevolucao().toString()  : "");
            obj.put("status",         e.getStatus() != null ? e.getStatus() : "");

            return ResponseEntity.ok(obj);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── CRIAR ────────────────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, String> dados) {
        try {
            int idUsuario = Integer.parseInt(dados.get("idUsuario"));
            int idLivro   = Integer.parseInt(dados.get("idLivro"));

            Usuario usuario = usuarioDAO.read(idUsuario);
            Livro   livro   = livroDAO.read(idLivro);

            if (usuario == null) return ResponseEntity.status(404).body(Map.of("erro", "Usuário não encontrado"));
            if (livro   == null) return ResponseEntity.status(404).body(Map.of("erro", "Livro não encontrado"));

            Emprestimo e = new Emprestimo(
                    idUsuario,
                    idLivro,
                    LocalDate.parse(dados.get("dataEmprestimo")),
                    LocalDate.parse(dados.get("dataDevolucao"))
            );

            int novoId = emprestimoDAO.create(e);
            return ResponseEntity.ok(Map.of("id", novoId));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── ATUALIZAR ────────────────────────────────────────────────────────────
    @PutMapping
    public ResponseEntity<?> atualizar(
            @RequestParam int id,
            @RequestBody Map<String, String> dados) {
        try {
            Emprestimo existente = emprestimoDAO.read(id);

            if (existente == null) {
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            }

            existente.setIdUsuario(Integer.parseInt(dados.get("idUsuario")));
            existente.setIdLivro(Integer.parseInt(dados.get("idLivro")));
            existente.setDataEmprestimo(LocalDate.parse(dados.get("dataEmprestimo")));
            existente.setDataDevolucao(LocalDate.parse(dados.get("dataDevolucao")));

            emprestimoDAO.update(existente);
            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── DEVOLVER ─────────────────────────────────────────────────────────────
    @PutMapping("/devolver")
    public ResponseEntity<?> devolver(@RequestParam int id) {
        try {
            Emprestimo e = emprestimoDAO.read(id);

            if (e == null) {
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            }

            e.setStatus("Devolvido");
            emprestimoDAO.update(e);
            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }

    // ─── DELETAR ──────────────────────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<?> deletar(@RequestParam int id) {
        try {
            boolean ok = emprestimoDAO.delete(id);

            if (!ok) {
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            }

            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
        }
    }
}