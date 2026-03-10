package com.bibliotech;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.model.Livro;

public class Main {
    public static void main(String[] args) throws IOException {

    	Scanner sc = new Scanner(System.in);
    	sc.useLocale(java.util.Locale.US);
        LivroDAO dao = new LivroDAO();
        int opcao;

        do {
            System.out.println("\n1 - Criar");
            System.out.println("2 - Buscar por ID");
            System.out.println("3 - Atualizar");
            System.out.println("4 - Deletar");
            System.out.println("5 - Listar");
            System.out.println("0 - Sair");

            opcao = sc.nextInt();
            sc.nextLine();

            switch (opcao) {
                case 1:
                    System.out.println("Criar livro");
                    System.out.print("Título: ");
                    String titulo = sc.nextLine();

                    System.out.print("Resumo: ");
                    String resumo = sc.nextLine();

                    System.out.print("Preço: ");
                    float preco = sc.nextFloat();
                    sc.nextLine();

                    System.out.print("Data de Publicação (dd/MM/yyyy): ");
                    LocalDate data = null;
                    try{
                        String dataStr = sc.nextLine();
                        data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    }catch(Exception e){
                        System.out.println("Formato inválido.");
                    }

                    System.out.print("Gêneros: ");
                    String[] generos = sc.nextLine().split(",");

                    Livro novo = new Livro(titulo, resumo, preco, data, generos);
                    int id = dao.create(novo);

                    System.out.println("Livro criado com ID: " + id);
                    break;

                case 2:
                    System.out.println("Buscar livro");
                    System.out.print("ID: ");
                    int idBusca = sc.nextInt();
                    sc.nextLine();

                    Livro encontraLivro = dao.read(idBusca);
                    imprimirLivro(encontraLivro);
                    break;

                case 3:
                    System.out.println("Atualizar livro");
                    System.out.print("ID: ");
                    int idAtualiza = sc.nextInt();
                    sc.nextLine();

                    Livro atualizaLivro = dao.read(idAtualiza);

                    if (atualizaLivro == null) {
                        System.out.println("Livro não encontrado.");
                        break;
                    }

                    imprimirLivro(atualizaLivro);

                    System.out.println("\nO que deseja editar?");
                    System.out.println("1 - Título");
                    System.out.println("2 - Resumo");
                    System.out.println("3 - Preço");
                    System.out.println("4 - Data");
                    System.out.println("5 - Gêneros");
                    System.out.println("6 - Tudo");

                    int escolha = sc.nextInt();
                    sc.nextLine();

                    switch(escolha){

                        case 1:
                            System.out.print("Novo título: ");
                            atualizaLivro.setTitulo(sc.nextLine());
                            break;

                        case 2:
                            System.out.print("Novo resumo: ");
                            atualizaLivro.setResumo(sc.nextLine());
                            break;

                        case 3:
                            System.out.print("Novo preço: ");
                            atualizaLivro.setPreco(sc.nextFloat());
                            sc.nextLine();
                            break;

                        case 4:
                            System.out.print("Nova data (dd/MM/yyyy): ");
                            atualizaLivro.setDataPublicacao(
                                LocalDate.parse(sc.nextLine(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            );
                            break;

                        case 5:
                            System.out.print("Novos gêneros: ");
                            atualizaLivro.setGeneros(sc.nextLine().split(","));
                            break;

                        case 6:
                            System.out.print("Novo título: ");
                            atualizaLivro.setTitulo(sc.nextLine());

                            System.out.print("Novo resumo: ");
                            atualizaLivro.setResumo(sc.nextLine());

                            System.out.print("Novo preço: ");
                            atualizaLivro.setPreco(sc.nextFloat());
                            sc.nextLine();

                            System.out.print("Nova data (dd/MM/yyyy): ");
                            atualizaLivro.setDataPublicacao(
                                LocalDate.parse(sc.nextLine(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            );

                            System.out.print("Novos gêneros: ");
                            atualizaLivro.setGeneros(sc.nextLine().split(","));
                            break;
                    }

                    if (dao.update(atualizaLivro)) {
                        System.out.println("Livro atualizado.");
                    } else {
                        System.out.println("Erro ao atualizar.");
                    }
                    break;

                case 4:
                    System.out.println("Deletar livro");
                    System.out.print("ID para deletar: ");
                    int idDeleta = sc.nextInt();
                    sc.nextLine();

                    if (dao.delete(idDeleta)) {
                        System.out.println("Livro deletado.");
                    } else {
                        System.out.println("Livro não encontrado.");
                    }
                    break;

                case 5:
                    dao.listar();
                    break;
            }
        } while (opcao != 0);
        sc.close();
    }

    public static void imprimirLivro(Livro l) {
        if (l == null) {
            System.out.println("Livro inexistente.");
            return;
        }
        System.out.print("ID: " + l.getId());
        System.out.print(" | Título: " + l.getTitulo());
        System.out.print(" | Resumo: " + l.getResumo());
        System.out.print(" | Preço: R$" + l.getPreco());
        System.out.print(" | Data: " + l.getDataPublicacao());
        System.out.print(" | Gêneros: ");

        if (l.getGeneros() != null) {
            for (String g : l.getGeneros()) {
                System.out.print(g + " ");
            }
        }
        System.out.println();
    }
}
