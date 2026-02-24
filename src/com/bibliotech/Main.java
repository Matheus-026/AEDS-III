package com.bibliotech;

import java.io.IOException;
import java.io.File;
import java.time.LocalDate;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;

public class Main {
    public static void main(String[] args) throws IOException {
        
        // Reset do arquivo para teste limpo
        File f = new File("livros.dat");
        if(f.exists()) f.delete();

        LivroDAO dao = new LivroDAO();
         
        System.out.println("--- 1. TESTE: CREATE ---");
        Livro l1 = new Livro("Dom Casmurro", "Ciúmes e Bentinho.", 120.50f, LocalDate.of(1899, 1, 1), new String[]{"Realismo", "Romance Psicológico"});
        int id1 = dao.create(l1);
        System.out.println("Livro 1 criado com ID: " + id1);
        System.out.println();

        System.out.println("--- 2. TESTE: UPDATE (Alteração no local) ---");
        // Vamos alterar o título para algo de tamanho similar ou menor
        l1.setTitulo("Casmurro"); 
        l1.setPreco(110.00f);
        if(dao.update(l1)) {
            System.out.println("Update 1 concluído. Verificando dados:");
            imprimirLivro(dao.read(id1));
        }

        System.out.println("\n--- 3. TESTE: UPDATE (Movendo para o fim do arquivo) ---");
        // Título muito maior: forçará a criação de um novo registro no fim do arquivo
        l1.setTitulo("Dom Casmurro - Edição Especial de Luxo Comentada por Especialistas");
        if(dao.update(l1)) {
            System.out.println("Update 2 (registro maior) concluído. Verificando dados:");
            imprimirLivro(dao.read(id1));
        }

        System.out.println("\n--- 4. TESTE: DELETE ---");
        if(dao.delete(id1)) {
            System.out.println("Registro " + id1 + " excluído logicamente.");
        }
        
        Livro tentativaLeitura = dao.read(id1);
        if(tentativaLeitura == null) {
            System.out.println("Sucesso: O sistema ignorou o registro excluído (Lápide funcionou).");
        } else {
            System.out.println("Erro: O sistema ainda está lendo o registro deletado!");
        }

        System.out.println("\n--- 5. TESTE: REUTILIZAÇÃO DO ARQUIVO ---");
        Livro l2 = new Livro("A Moreninha", "Romance romântico.", 45.00f, LocalDate.of(1844, 1, 1), new String[]{"Romantismo"});
        int id2 = dao.create(l2);
        System.out.println("Novo livro criado com ID: " + id2);
        imprimirLivro(dao.read(id2));
    }

    public static void imprimirLivro(Livro l) {
        if(l == null) {
            System.out.println("Livro inexistente.");
            return;
        }
        
        System.out.print("ID: " + l.getId());
        System.out.print(" | Título: " + l.getTitulo());
        System.out.print(" | Preço: R$" + l.getPreco());
        System.out.print(" | Gêneros: ");
    
        if(l.getGeneros() != null) {
            for(String g : l.getGeneros()) {
                System.out.print(g + " ");
            }
        }
    
        System.out.println();
    }
}
