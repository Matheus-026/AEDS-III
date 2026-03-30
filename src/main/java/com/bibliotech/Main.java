package com.bibliotech;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import com.bibliotech.dao.LivroDAO;
import com.bibliotech.dao.UsuarioDAO;
import com.bibliotech.model.Livro;
import com.bibliotech.model.Usuario;

public class Main {
    private static void menuUsuarios(Scanner sc, UsuarioDAO dao) throws IOException {
        int opcao;
        do {
            System.out.println("\n--- GESTÃO DE USUÁRIOS ---");
            System.out.println("1 - Criar Usuário (Standard)");
            System.out.println("2 - Listar Todos (Front-end Simulation)");
            System.out.println("3 - Deletar Usuário");
            System.out.println("0 - Voltar");
            System.out.print("Opção: ");
            opcao = sc.nextInt();
            sc.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome: ");
                    String nome = sc.nextLine();
                    System.out.print("Email: ");
                    String email = sc.nextLine();
                    System.out.print("Senha: ");
                    String senha = sc.nextLine();
                    
                    // Como combinado, novos cadastros padrão são "Standard"
                    Usuario novo = new Usuario(nome, email, senha, "Standard");
                    int id = dao.create(novo);
                    System.out.println("Usuário criado com ID: " + id);
                    break;

                case 2:
                    dao.listar();
                    break;

                case 3:
                    System.out.print("ID para deletar: ");
                    int idDel = sc.nextInt();
                    if (dao.delete(idDel)) System.out.println("Usuário removido.");
                    else System.out.println("Não encontrado.");
                    break;

                case 0:
                    System.out.println("Retornando ao menu principal...");
                    break;
                    
                default:
                    System.out.println("Opção inválida.");
                    break;
            }
        } while (opcao != 0);
    }



    private static void menuLivros(Scanner sc, LivroDAO dao) throws IOException {
        int opcao = 0;
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

    public static void main(String[] args) throws IOException {

    	Scanner sc = new Scanner(System.in);
    	sc.useLocale(java.util.Locale.US);

        // Instancia os dois DAOs
        LivroDAO livroDao = new LivroDAO();
        UsuarioDAO usuarioDao = new UsuarioDAO(); 
        // No momento que UsuarioDAO é instanciado, se o arquivo for novo, 
        // o Admin padrão já é criado!

        int menuPrincipal;

        do {
            System.out.println("\n--- BIBLIOTECH ---");
            System.out.println("1 - Menu de Livros");
            System.out.println("2 - Menu de Usuários");
            System.out.println("0 - Sair");
            System.out.print("Escolha: ");
            menuPrincipal = sc.nextInt();
            sc.nextLine();

            if (menuPrincipal == 1) {
                menuLivros(sc, livroDao);
            } else if (menuPrincipal == 2) {
                menuUsuarios(sc, usuarioDao);
            }

        } while (menuPrincipal != 0);
        
        sc.close();
    }
}
