package com.bibliotech.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NavegacaoController {

    // URL Amigável: localhost:8080/
    @GetMapping("/")
    public String home() {
        return "index"; // Retorna o arquivo src/main/resources/templates/index.html
    }

    // URL Amigável: localhost:8080/login
    @GetMapping("/login") // Url pesquisada
    public String login() {
        return "login"; // Retorna o arquivo src/main/resources/templates/login.html
    }

    // URL Amigável: localhost:8080/cadastro
    @GetMapping("/cadastro") // Url pesquisada
    public String cadastro() {
        return "cadastro"; // Retorna o arquivo src/main/resources/templates/cadastro.html
    }

    // URL Amigável: localhost:8080/usuarios
    @GetMapping("/usuarios") // Url pesquisada
    public String listaUsuarios() {
        return "usuarios"; // Retorna o arquivo src/main/resources/templates/usuarios.html
    }

    // URL Amigável: localhost:8080/pesquisa
    @GetMapping("/pesquisa") // Url pesquisada
    public String pesquisa() {
        return "pesquisa_avancada"; // Retorna o arquivo src/main/resources/templates/pesquisa_avancada.html
    }

    // URL Amigável: localhost:8080/usuarios
    @GetMapping("/adm") // Url pesquisada
    public String adm() {
        return "page_adm"; // Retorna o arquivo src/main/resources/templates/page_adm.html
    }

    // URL Amigável: localhost:8080/livros
    @GetMapping("/livros") // Url pesquisada
    public String listaLivros() {
        return "livro"; // Retorna o arquivo src/main/resources/templates/livro.html
    }

    // URL Amigável: localhost:8080/emprestimo
    @GetMapping("/emprestimos") // Url pesquisada
    public String listaEmprestimo() {
        return "emprestimo"; // Retorna o arquivo src/main/resources/templates/livro.html
    }

    // URL Amigável: localhost:8080/emprestimo
    @GetMapping("/autores") // Url pesquisada
    public String listaAutores() {
        return "autor"; // Retorna o arquivo src/main/resources/templates/autor.html
    }
}