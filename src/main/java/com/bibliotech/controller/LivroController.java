package com.bibliotech.controller;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;
import com.bibliotech.service.OrdenacaoExternaLivros;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/livros")
@CrossOrigin(origins = "*")
public class LivroController {
    
    @Autowired
    private OrdenacaoExternaLivros ordenacaoService;
    
    private LivroDAO livroDAO;

    public LivroController() throws IOException {
        livroDAO = new LivroDAO();
    }

    // =========================
    // LISTAR TODOS
    // =========================
    @GetMapping
    public List<Map<String, Object>> listar() throws IOException {

        List<Map<String, Object>> lista = new ArrayList<>();

        java.io.RandomAccessFile arquivo = new java.io.RandomAccessFile("data/livros.dat", "rw");

        arquivo.seek(8);

        while (arquivo.getFilePointer() < arquivo.length()) {

            boolean ativo = arquivo.readBoolean();
            int tamanho = arquivo.readInt();

            byte[] dados = new byte[tamanho];
            arquivo.readFully(dados);

            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);

                Map<String, Object> obj = new HashMap<>();
                obj.put("id", l.getId());
                obj.put("titulo", l.getTitulo());
                obj.put("resumo", l.getResumo());
                obj.put("preco", l.getPreco());
                obj.put("dataPublicacao", l.getDataPublicacao().toString());
                obj.put("generos", l.getGeneros());
                obj.put("idAutor", l.getIdAutor());

                lista.add(obj);
            }
        }

        return lista;
    }

    // =========================
    // BUSCAR POR ID
    // =========================
    @GetMapping("/{id}")
    public Map<String, Object> buscarPorId(@PathVariable int id) throws IOException {

        Livro l = livroDAO.read(id);

        if (l == null) {
            throw new RuntimeException("Livro não encontrado");
        }

        Map<String, Object> obj = new HashMap<>();
        obj.put("id", l.getId());
        obj.put("titulo", l.getTitulo());
        obj.put("resumo", l.getResumo());
        obj.put("preco", l.getPreco());
        obj.put("dataPublicacao", l.getDataPublicacao().toString());
        obj.put("generos", l.getGeneros());
        obj.put("idAutor", l.getIdAutor());

        return obj;
    }

    // =========================
    // CRIAR
    // =========================
    @SuppressWarnings("unchecked")
    @PostMapping
    public Map<String, Object> criar(@RequestBody Map<String, Object> dados) throws IOException {

        String titulo = (String) dados.get("titulo");
        String resumo = (String) dados.get("resumo");
        double preco = Double.parseDouble(dados.get("preco").toString());
        LocalDate data = LocalDate.parse((String) dados.get("dataPublicacao"));
        int idAutor = Integer.parseInt(dados.get("idAutor").toString());
        ;

        List<String> generosList = (List<String>) dados.get("generos");
        String[] generos = generosList.toArray(new String[0]);

        Livro l = new Livro(titulo, resumo, (float) preco, data, generos, idAutor);

        int id = livroDAO.create(l);

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", id);

        return resp;
    }

    // =========================
    // ATUALIZAR
    // =========================
    @SuppressWarnings("unchecked")
    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
            @PathVariable int id,
            @RequestBody Map<String, Object> dados) throws IOException {

        Livro l = livroDAO.read(id);

        if (l == null) {
            throw new RuntimeException("Livro não encontrado");
        }

        l.setTitulo((String) dados.get("titulo"));
        l.setResumo((String) dados.get("resumo"));
        l.setPreco(Float.parseFloat(dados.get("preco").toString()));
        l.setDataPublicacao(LocalDate.parse((String) dados.get("dataPublicacao")));
        l.setIdAutor(Integer.parseInt(dados.get("idAutor").toString()));

        List<String> generosList = (List<String>) dados.get("generos");
        l.setGeneros(generosList.toArray(new String[0]));

        livroDAO.update(l);

        return Map.of("ok", true);
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public Map<String, Object> deletar(@PathVariable int id) throws IOException {

        boolean ok = livroDAO.delete(id);

        if (!ok) {
            throw new RuntimeException("Livro não encontrado");
        }

        return Map.of("ok", true);
    }

    // =========================
    // ORDENAÇÃO
    // =========================
    @PostMapping("/ordenar")
    public ResponseEntity<String> ordenarLivros() {
        try {
            ordenacaoService.ordenarPorPreco();
            return ResponseEntity.ok("Livros ordenados com sucesso por preço!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Erro ao processar ordenação externa.");
        }
    }
}