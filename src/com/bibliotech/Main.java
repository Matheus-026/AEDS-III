package com.bibliotech;

import java.io.IOException;
import java.time.LocalDate;
import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;

public class Main{
	public static void main(String[]args) throws IOException {
		 LivroDAO dao = new LivroDAO();
		 
		 Livro livro = new Livro("Dom Casmurro", "Narrativa em primeira pessoa sobre memoria e ciume.", 120.50f, LocalDate.of(1899, 1, 1), "romance realista, realismo psicológico, romance impressionista");
		 
		 int id = dao.create(livro);
		 
		 System.out.println("Livro criado com ID: "+ id);
		 
		 Livro livro2 = new Livro(
				    "Memorias Postumas de Bras Cubas",
				    "Romance narrado por um defunto autor que revisita sua vida com ironia e critica social, explorando temas como vaidade, ambicao e hipocrisia da sociedade do seculo XIX.",
				    98.90f,
				    LocalDate.of(1881, 1, 1),
				    "romance realista, critica social, literatura brasileira"
				);

				int id2 = dao.create(livro2);

				System.out.println("Livro criado com ID: " + id2);
	}
	
	
}