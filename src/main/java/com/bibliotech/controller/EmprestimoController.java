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
@CrossOrigin(origins = "*")
public class EmprestimoController {

    private static EmprestimoDAO emprestimoDAO;
    private static UsuarioDAO    usuarioDAO;
    private static LivroDAO      livroDAO;

    static {
        try {
            emprestimoDAO = new EmprestimoDAO();
            usuarioDAO    = new UsuarioDAO();
            livroDAO      = new LivroDAO();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Tratamento global de exceções ────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleErro(Exception ex) {
        ex.printStackTrace();
        String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        return ResponseEntity.status(500).body(Map.of("erro", msg));
    }

    // =========================================================================
    // LISTAR TODOS
    // GET /api/emprestimos
    // =========================================================================
    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<Emprestimo> todos = emprestimoDAO.listAll();
            return ResponseEntity.ok(enriquecer(todos));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : "Erro interno"));
        }
    }

    // =========================================================================
    // BUSCAR POR ID
    // GET /api/emprestimos/{id}
    // =========================================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            Emprestimo e = emprestimoDAO.read(id);
            if (e == null)
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            return ResponseEntity.ok(enriquecerUm(e));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // BUSCAR POR USUÁRIO (N:N — lado usuario → livros)
    // GET /api/emprestimos/usuario/{idUsuario}
    // =========================================================================
    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<?> buscarPorUsuario(@PathVariable int idUsuario) {
        try {
            List<Emprestimo> lista = emprestimoDAO.buscarPorUsuario(idUsuario);
            return ResponseEntity.ok(enriquecer(lista));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // BUSCAR POR LIVRO (N:N — lado livro → usuários)
    // GET /api/emprestimos/livro/{idLivro}
    // =========================================================================
    @GetMapping("/livro/{idLivro}")
    public ResponseEntity<?> buscarPorLivro(@PathVariable int idLivro) {
        try {
            List<Emprestimo> lista = emprestimoDAO.buscarPorLivro(idLivro);
            return ResponseEntity.ok(enriquecer(lista));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // BUSCAR ATIVO POR PAR (chave composta — indicador visual no modal)
    // GET /api/emprestimos/ativo?idUsuario=X&idLivro=Y
    // Retorna 200 + empréstimo se houver ativo, 404 se par livre.
    // =========================================================================
    @GetMapping("/ativo")
    public ResponseEntity<?> buscarAtivoPorPar(
            @RequestParam int idUsuario,
            @RequestParam int idLivro) {
        try {
            Emprestimo e = emprestimoDAO.buscarAtivoPorPar(idUsuario, idLivro);
            if (e == null)
                return ResponseEntity.status(404).body(Map.of("livre", true));
            return ResponseEntity.ok(enriquecerUm(e));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // CRIAR
    // POST /api/emprestimos
    // =========================================================================
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, String> dados) {
        try {
            int idUsuario = Integer.parseInt(dados.get("idUsuario"));
            int idLivro   = Integer.parseInt(dados.get("idLivro"));

            Usuario usuario = usuarioDAO.read(idUsuario);
            Livro   livro   = livroDAO.read(idLivro);

            if (usuario == null)
                return ResponseEntity.status(404).body(Map.of("erro", "Usuário não encontrado"));
            if (livro == null)
                return ResponseEntity.status(404).body(Map.of("erro", "Livro não encontrado"));

            Emprestimo e = new Emprestimo(
                idUsuario,
                idLivro,
                LocalDate.parse(dados.get("dataEmprestimo")),
                LocalDate.parse(dados.get("dataDevolucao"))
            );

            int novoId = emprestimoDAO.create(e);
            return ResponseEntity.ok(Map.of("id", novoId));

        } catch (IllegalStateException ex) {
            // par duplicado ativo — mensagem amigável para o front-end
            return ResponseEntity.status(409).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // ATUALIZAR
    // PUT /api/emprestimos/{id}
    // =========================================================================
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable int id,
            @RequestBody Map<String, String> dados) {
        try {
            Emprestimo existente = emprestimoDAO.read(id);
            if (existente == null)
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));

            if (dados.containsKey("idUsuario"))
                existente.setIdUsuario(Integer.parseInt(dados.get("idUsuario")));
            if (dados.containsKey("idLivro"))
                existente.setIdLivro(Integer.parseInt(dados.get("idLivro")));
            if (dados.containsKey("dataEmprestimo"))
                existente.setDataEmprestimo(LocalDate.parse(dados.get("dataEmprestimo")));
            if (dados.containsKey("dataDevolucao"))
                existente.setDataDevolucao(LocalDate.parse(dados.get("dataDevolucao")));
            if (dados.containsKey("status"))
                existente.setStatus(dados.get("status"));

            emprestimoDAO.update(existente);
            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // DEVOLVER POR ID
    // PUT /api/emprestimos/{id}/devolver
    // =========================================================================
    @PutMapping("/{id}/devolver")
    public ResponseEntity<?> devolver(@PathVariable int id) {
        try {
            Emprestimo e = emprestimoDAO.read(id);
            if (e == null)
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));

            e.setStatus("Devolvido");
            emprestimoDAO.update(e);
            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // DELETAR
    // DELETE /api/emprestimos/{id}
    // =========================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable int id) {
        try {
            boolean ok = emprestimoDAO.delete(id);
            if (!ok)
                return ResponseEntity.status(404).body(Map.of("erro", "Empréstimo não encontrado"));
            return ResponseEntity.ok(Map.of("ok", true));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("erro", ex.getMessage()));
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /** Enriquece um único empréstimo com nome do usuário e título do livro. */
    private Map<String, Object> enriquecerUm(Emprestimo e) throws IOException {
        Usuario u = usuarioDAO.read(e.getIdUsuario());
        Livro   l = livroDAO.read(e.getIdLivro());

        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("id",             e.getId());
        obj.put("idUsuario",      e.getIdUsuario());
        obj.put("idLivro",        e.getIdLivro());
        obj.put("nomeUsuario",    u != null ? u.getNome()   : "Desconhecido");
        obj.put("tituloLivro",    l != null ? l.getTitulo() : "Desconhecido");
        obj.put("dataEmprestimo", e.getDataEmprestimo() != null ? e.getDataEmprestimo().toString() : "");
        obj.put("dataDevolucao",  e.getDataDevolucao()  != null ? e.getDataDevolucao().toString()  : "");
        obj.put("status",         e.getStatus() != null ? e.getStatus() : "");
        return obj;
    }

    /** Enriquece uma lista de empréstimos, ignorando registros com erro. */
    private List<Map<String, Object>> enriquecer(List<Emprestimo> lista) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Emprestimo e : lista) {
            try {
                resultado.add(enriquecerUm(e));
            } catch (Exception ex) {
                System.err.println("[enriquecer] Empréstimo id=" + e.getId() + " ignorado: " + ex.getMessage());
            }
        }
        return resultado;
    }
}