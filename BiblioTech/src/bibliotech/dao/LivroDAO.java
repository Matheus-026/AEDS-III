package bibliotech.dao;

import bibliotech.model.Livro;
import bibliotech.core.Arquivo;

public class LivroDAO{
    private Arquivo<Livro> arqLivros;
    
    public LivroDAO() throws Exception{
        arqLivros = new Arquivo<>("livros", Livro.class.getConstructor());
    }

    public int criar(Livro livro) throws Exception{
        return arqLivros.create(livro);
    }

    public Livro buscarPorId(int id) throws Exception{
        return arqLivros.read(id);
    }

    public boolean atualizar(Livro livro) throws Exception{
        return arqLivros.update(livro);
    }

    public boolean excluir(int id) throws Exception{
        return arqLivros.delete(id);
    }
}

