package com.bibliotech.controller;

import java.io.IOException;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.dao.AutorDAO;
import com.bibliotech.model.Livro;
import com.bibliotech.model.Autor;
import com.bibliotech.util.KMP;
import com.bibliotech.util.BoyerMoore;

@RestController
@RequestMapping("/livros")
@CrossOrigin
public class PesquisaController {

    private LivroDAO dao;
    private AutorDAO autorDAO;

    public PesquisaController() throws IOException {
        dao      = new LivroDAO();
        autorDAO = new AutorDAO();
    }

    // BUSCA AVANÇADA 
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

         return dao.buscaAvancada(titulo, genero, precoMin, precoMax, autorNome, autorTelefone,
                                  usuarioNome, usuarioEmail, usuarioStatus,
                                  emprestimoLivro, emprestimoUsuario, dataInicial, dataFinal);
    }


    // BUSCA POR PADRÃO — KMP e Boyer-Moore
    // Campos: titulo, autor, genero, todos.
   
    
    @GetMapping("/busca-padrao")
    public ResponseEntity<?> buscaPorPadrao(
            @RequestParam(name = "padrao") String padrao,
            @RequestParam(name = "algoritmo", defaultValue = "KMP") String algoritmo,
            @RequestParam(name = "campo", defaultValue = "titulo") String campo) {

        if (padrao == null || padrao.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("erro", "O parâmetro 'padrao' é obrigatório."));
        }

        if (!"KMP".equalsIgnoreCase(algoritmo) && !"BM".equalsIgnoreCase(algoritmo)) {
            return ResponseEntity.badRequest()
                .body(Map.of("erro",
                    "Algoritmo '" + algoritmo + "' inválido. Use 'KMP' ou 'BM'."));
        }

        boolean buscaTitulo = campo.equals("titulo") || campo.equals("todos");
        boolean buscaAutor  = campo.equals("autor")  || campo.equals("todos");
        boolean buscaGenero = campo.equals("genero") || campo.equals("todos");

        try {
            List<Livro> todos = dao.readAll();
            List<Map<String, Object>> resultado = new ArrayList<>();

            for (Livro l : todos) {

                Autor autor = null;
                try { autor = autorDAO.read(l.getIdAutor()); } catch (Exception ignored) {}

                String nomeAutor = autor != null ? autor.getNome() : "";
                String generosTexto = l.getGeneros() != null
                        ? String.join(", ", l.getGeneros()) : "";

                List<Integer> ocorrenciasTitulo = new ArrayList<>();
                List<Integer> ocorrenciasAutor  = new ArrayList<>();
                List<Integer> ocorrenciasGenero = new ArrayList<>();

                if (buscaTitulo && l.getTitulo() != null) {
                    if ("KMP".equalsIgnoreCase(algoritmo)) ocorrenciasTitulo = KMP.buscar(l.getTitulo(), padrao);
                    else ocorrenciasTitulo = BoyerMoore.buscar(l.getTitulo(), padrao);
                }
                if (buscaAutor && !nomeAutor.isEmpty()) {
                    if ("KMP".equalsIgnoreCase(algoritmo)) ocorrenciasAutor = KMP.buscar(nomeAutor, padrao);
                    else ocorrenciasAutor = BoyerMoore.buscar(nomeAutor, padrao);
                }
                if (buscaGenero && !generosTexto.isEmpty()) {
                    if ("KMP".equalsIgnoreCase(algoritmo)) ocorrenciasGenero = KMP.buscar(generosTexto, padrao);
                    else ocorrenciasGenero = BoyerMoore.buscar(generosTexto, padrao);
                }

                int totalOcorrencias = ocorrenciasTitulo.size()
                                      + ocorrenciasAutor.size()
                                      + ocorrenciasGenero.size();

                if (totalOcorrencias > 0) {
                    Map<String, Object> obj = new LinkedHashMap<>();
                    obj.put("id",                l.getId());
                    obj.put("titulo",            l.getTitulo());
                    obj.put("preco",             l.getPreco());
                    obj.put("dataPublicacao",    l.getDataPublicacao() != null
                                                    ? l.getDataPublicacao().toString() : "");
                    obj.put("generos",           l.getGeneros());
                    obj.put("idAutor",           l.getIdAutor());
                    obj.put("nomeAutor",         !nomeAutor.isEmpty() ? nomeAutor : "Desconhecido");
                    obj.put("ocorrenciasTitulo", ocorrenciasTitulo);
                    obj.put("ocorrenciasAutor",  ocorrenciasAutor);
                    obj.put("ocorrenciasGenero", ocorrenciasGenero);
                    obj.put("totalOcorrencias",  totalOcorrencias);

                    resultado.add(obj);
                }
            }

            resultado.sort((a, b) ->
                Integer.compare((int) b.get("totalOcorrencias"), (int) a.get("totalOcorrencias"))
            );

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("padrao",          padrao);
            resposta.put("algoritmo",       algoritmo.toUpperCase());
            resposta.put("campo",           campo);
            resposta.put("totalResultados", resultado.size());
            resposta.put("resultados",      resultado);

            return ResponseEntity.ok(resposta);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : "Erro interno"));
        }
    }
}
