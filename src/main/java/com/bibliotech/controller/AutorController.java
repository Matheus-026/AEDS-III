package com.bibliotech.controller;

import com.bibliotech.dao.AutorDAO;
import com.bibliotech.model.Autor;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

@RestController
@RequestMapping("/api/autores")
@CrossOrigin
public class AutorController {

    private AutorDAO autorDAO;

    public AutorController() throws IOException {
        autorDAO = new AutorDAO();
    }

    // LISTAR
    @GetMapping
    public List<Map<String, Object>> listar() throws IOException {

        List<Map<String, Object>> lista = new ArrayList<>();

        try (RandomAccessFile arq = new RandomAccessFile("data/autores.dat", "rw")) {
			arq.seek(8);

			while (arq.getFilePointer() < arq.length()) {

			    boolean ativo = arq.readBoolean();
			    int tam = arq.readInt();

			    byte[] dados = new byte[tam];
			    arq.readFully(dados);

			    if (ativo) {
			        Autor a = new Autor();
			        a.fromByteArray(dados);

			        Map<String, Object> obj = new HashMap<>();
			        obj.put("id", a.getId());
			        obj.put("nome", a.getNome());
			        obj.put("telefone", a.getTelefone());
			        obj.put("biografia", a.getBiografia());

			        lista.add(obj);
			    }
			}
		}

        return lista;
    }

    // BUSCAR
    @GetMapping("/{id}")
    public Autor buscar(@PathVariable int id) throws IOException {
        return autorDAO.read(id);
    }

    // CRIAR
    @PostMapping
    public Map<String, Object> criar(@RequestBody Map<String, String> dados) throws IOException {

        Autor a = new Autor(
            dados.get("nome"),
            dados.get("telefone"),
            dados.get("biografia")
        );

        int id = autorDAO.create(a);

        return Map.of("id", id);
    }

    // UPDATE
    @PutMapping("/{id}")
    public Map<String, Object> atualizar(
        @PathVariable int id,
        @RequestBody Map<String, String> dados
    ) throws IOException {

        Autor a = autorDAO.read(id);

        a.setNome(dados.get("nome"));
        a.setTelefone(dados.get("telefone"));
        a.setBiografia(dados.get("biografia"));

        autorDAO.update(a);

        return Map.of("ok", true);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public Map<String, Object> deletar(@PathVariable int id) throws IOException {

        autorDAO.delete(id);

        return Map.of("ok", true);
    }
}