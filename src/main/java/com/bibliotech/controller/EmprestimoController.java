package com.bibliotech.controller;

import com.bibliotech.dao.EmprestimoDAO;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Emprestimo;
import com.bibliotech.model.Livro;
import com.bibliotech.model.Usuario;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/emprestimos")
@CrossOrigin
public class EmprestimoController {

    private EmprestimoDAO emprestimoDAO;
    private UsuarioDAO usuarioDAO;
    private LivroDAO livroDAO;

    public EmprestimoController() throws IOException {
        emprestimoDAO = new EmprestimoDAO();
        usuarioDAO = new UsuarioDAO();
        livroDAO = new LivroDAO();
    }

    // 🔹 LISTAR TODOS
    @GetMapping
    public List<Map<String, Object>> listar() throws IOException {

        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Emprestimo e : emprestimoDAO.listAll()) {

            Usuario u = usuarioDAO.read(e.getIdUsuario());
            Livro l = livroDAO.read(e.getIdLivro());

            Map<String, Object> obj = new HashMap<>();

            obj.put("id", e.getId());
            obj.put("idUsuario", e.getIdUsuario());
            obj.put("idLivro", e.getIdLivro());
            obj.put("nomeUsuario", u != null ? u.getNome() : "Desconhecido");
            obj.put("tituloLivro", l != null ? l.getTitulo() : "Desconhecido");
            obj.put("dataEmprestimo", e.getDataEmprestimo().toString());
            obj.put("dataDevolucao", e.getDataDevolucao().toString());
            obj.put("status", e.getStatus());

            resposta.add(obj);
        }

        return resposta;
    }

    // 🔹 BUSCAR POR ID
    @GetMapping(params = "id")
    public Map<String, Object> buscarPorId(@RequestParam int id) throws IOException {

        Emprestimo e = emprestimoDAO.read(id);

        if (e == null) {
            throw new RuntimeException("Empréstimo não encontrado");
        }

        Usuario u = usuarioDAO.read(e.getIdUsuario());
        Livro l = livroDAO.read(e.getIdLivro());

        Map<String, Object> obj = new HashMap<>();

        obj.put("id", e.getId());
        obj.put("idUsuario", e.getIdUsuario());
        obj.put("idLivro", e.getIdLivro());
        obj.put("nomeUsuario", u != null ? u.getNome() : "Desconhecido");
        obj.put("tituloLivro", l != null ? l.getTitulo() : "Desconhecido");
        obj.put("dataEmprestimo", e.getDataEmprestimo().toString());
        obj.put("dataDevolucao", e.getDataDevolucao().toString());
        obj.put("status", e.getStatus());

        return obj;
    }

    // 🔹 CRIAR
    @PostMapping
    public Map<String, Object> criar(@RequestBody Map<String, String> dados) throws Exception {

        int idUsuario = Integer.parseInt(dados.get("idUsuario"));
        int idLivro = Integer.parseInt(dados.get("idLivro"));

        Usuario usuario = usuarioDAO.read(idUsuario);
        Livro livro = livroDAO.read(idLivro);

        if (usuario == null) throw new Exception("Usuário não encontrado");
        if (livro == null) throw new Exception("Livro não encontrado");

        Emprestimo e = new Emprestimo(
                idUsuario,
                idLivro,
                LocalDate.parse(dados.get("dataEmprestimo")),
                LocalDate.parse(dados.get("dataDevolucao"))
        );

        int novoId = emprestimoDAO.create(e);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", novoId);
        return resp;
    }

    // 🔹 ATUALIZAR
    @PutMapping
    public Map<String, Object> atualizar(
            @RequestParam int id,
            @RequestBody Map<String, String> dados) throws Exception {

        Emprestimo existente = emprestimoDAO.read(id);

        if (existente == null) {
            throw new Exception("Empréstimo não encontrado");
        }

        existente.setIdUsuario(Integer.parseInt(dados.get("idUsuario")));
        existente.setIdLivro(Integer.parseInt(dados.get("idLivro")));
        existente.setDataEmprestimo(LocalDate.parse(dados.get("dataEmprestimo")));
        existente.setDataDevolucao(LocalDate.parse(dados.get("dataDevolucao")));

        emprestimoDAO.update(existente);

        return Map.of("ok", true);
    }

    // 🔹 DEVOLVER
    @PutMapping("/devolver")
    public Map<String, Object> devolver(@RequestParam int id) throws IOException {

        Emprestimo e = emprestimoDAO.read(id);

        if (e == null) {
            throw new RuntimeException("Empréstimo não encontrado");
        }

        e.setStatus("Devolvido");
        emprestimoDAO.update(e);

        return Map.of("ok", true);
    }

    // 🔹 DELETE
    @DeleteMapping
    public Map<String, Object> deletar(@RequestParam int id) throws IOException {

        boolean ok = emprestimoDAO.delete(id);

        if (!ok) {
            throw new RuntimeException("Empréstimo não encontrado");
        }

        return Map.of("ok", true);
    }
}