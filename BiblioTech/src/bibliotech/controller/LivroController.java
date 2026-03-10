package bibliotech.controller;

import bibliotech.dao.LivroDAO;
import bibliotech.model.Livro;

public class LivroController{
    private LivroDAO livroDAO;

    public LivroController() throws Exception{
        this.livroDAO = new LivroDAO();
    }

    public void adicionar(Livro livro) throws Exception{
        if(livro.getPreco() < 0){
            System.out.println("Erro: O preço do livro não pode ser negativo.");
            return;
        }
        int idGerado = livroDAO.criar(livro);
        System.out.println("Livro cadastrado com sucesso! ID: " + idGerado);
    }

    public Livro buscar(int id) throws Exception{
        return livroDAO.buscarPorId(id);
    }

    public boolean atualizar(Livro livro) throws Exception{
        return livroDAO.atualizar(livro);
    }

    public boolean excluir(int id) throws Exception{
        return livroDAO.excluir(id);
    }
}