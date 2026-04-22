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
             @RequestParam(name = "max", defaultValue = "-1") float precoMax,
             @RequestParam(name = "autorNome", required = false) String autorNome,
             @RequestParam(name = "autorTelefone", required = false) String autorTelefone,
             @RequestParam(name = "usuarioNome", required = false) String usuarioNome,
             @RequestParam(name = "usuarioEmail", required = false) String usuarioEmail,
             @RequestParam(name = "usuarioStatus", required = false) String usuarioStatus,
             @RequestParam(name = "emprestimoLivro", required = false) String emprestimoLivro,
             @RequestParam(name = "emprestimoUsuario", required = false) String emprestimoUsuario,
             @RequestParam(name = "dataInicial", required = false) String dataInicial,
             @RequestParam(name = "dataFinal", required = false) String dataFinal) 
             throws IOException {

         // Agora passamos TODOS os parâmetros capturados para o seu DAO
         return dao.buscaAvancada(titulo, genero, precoMin, precoMax, autorNome, autorTelefone, 
                                  usuarioNome, usuarioEmail, usuarioStatus, 
                                  emprestimoLivro, emprestimoUsuario, dataInicial, dataFinal);
    }
}