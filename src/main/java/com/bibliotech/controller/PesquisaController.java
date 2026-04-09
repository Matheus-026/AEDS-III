package com.bibliotech.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;

@RestController
@RequestMapping("/livros")
@CrossOrigin
public class PesquisaController {

    private LivroDAO dao;

    public PesquisaController() throws IOException {
        dao = new LivroDAO();
    }

    @GetMapping("/busca")
    public List<Livro> busca(
            @RequestParam(name = "titulo", required = false) String titulo,
            @RequestParam(name = "genero", required = false) String genero,
            @RequestParam(name = "min", defaultValue = "-1") float precoMin,
            @RequestParam(name = "max", defaultValue = "-1") float precoMax) 
            throws IOException {

        return dao.buscaAvancada(titulo, genero, precoMin, precoMax);
    }
}