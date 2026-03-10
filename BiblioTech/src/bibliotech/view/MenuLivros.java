package bibliotech.view;

import bibliotech.controller.LivroController;
import bibliotech.model.Livro;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MenuLivros{
    private LivroController controller;
    private Scanner console;

    public MenuLivros(){
        try{
            this.controller = new LivroController();
            this.console = new Scanner(System.in);
        }catch(Exception e){
            System.out.println("Erro ao ligar moto do banco: " + e.getMessage());
        }
    }

    public void iniciar(){
        int opcao = -1;
        while(opcao != 0){
            System.out.println("\n--- BiblioTech: Gerenciamento de Livros ---");
            System.out.println("1 - Cadastrar Livro");
            System.out.println("2 - Buscar Livro por ID");
            System.out.println("3 - Atualizar Livro");
            System.out.println("4 - Excluir Livro");
            System.out.println("0 - Sair");
            System.out.print("Escolha uma opção: ");

            try {
                opcao = Integer.parseInt(console.nextLine());
                switch (opcao) {
                    case 1: cadastrar(); break;
                    case 2: buscar(); break;
                    case 3: atualizar(); break;
                    case 4: excluir(); break;
                    case 0: System.out.println("Saindo do sistema..."); break;
                    default: System.out.println("Opção inválida!");
                }
            } catch (NumberFormatException e) {
                System.out.println("Por favor, digite um número válido.");
            }
        }
    }

    private void cadastrar(){
        try{
            System.out.print("Título: ");
            String titulo = console.nextLine();

            System.out.print("Resumo: ");
            String resumo = console.nextLine();

            System.out.print("Preço: R$ ");
            float preco = Float.parseFloat(console.nextLine());

            System.out.print("Data de Publicação (DD/MM/AAAA): ");
            String strData = console.nextLine();
            LocalDate data = LocalDate.parse(strData, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            System.out.print("Gêneros (separados por vírgula): ");
            String strGeneros = console.nextLine();
            String[] generos = strGeneros.split(",");
            for(int i = 0; i < generos.length; i++){
                generos[i] = generos[i].trim();
            }

            Livro novoLivro = new Livro(titulo, resumo, preco, data, generos);
            controller.adicionar(novoLivro);
        } catch (Exception e) {
            System.out.println("Erro ao ler os dados. Verifique a formatação (ex: data em DD/MM/AAAA).");
        }
    }

    private void buscar(){
        System.out.print("Digite o ID do livro: ");
        try{
            int id = Integer.parseInt(console.nextLine());
            Livro livro = controller.buscar(id);
            
            if(livro != null){
                System.out.println("\n--- Livro Encontrado ---");
                System.out.println(livro.toString());
            }else{
                System.out.println("Nenhum livro ativo econtrado com o ID " + id);
            }
        }catch (Exception e) {
            System.out.println("Erro ao buscar livro: " + e.getMessage());
        }
    }

    private void atualizar() {
        System.out.println("Digite o ID do livro que deseja atualizar: ");
        try{
            int id = Integer.parseInt(console.nextLine());
            Livro livroExistente = controller.buscar(id);

            if(livroExistente == null){
                System.out.println("Livro não encontrado para atualização");
                return;
            }

            System.out.println("Livro atual: " + livroExistente.getTitulo());
            System.out.print("Novo título (ou ENTER para manter): ");
            String titulo = console.nextLine();
            if(!titulo.isEmpty()) livroExistente.setTitulo(titulo);

            System.out.print("Novo preço (ou ENTER para manter): R$ ");
            String precoStr = console.nextLine();
            if (!precoStr.isEmpty()) livroExistente.setPreco(Float.parseFloat(precoStr));

            if (controller.atualizar(livroExistente)) {
                System.out.println("Livro atualizado com sucesso!");
            } else {
                System.out.println("Erro ao atualizar no banco de dados.");
            }
        } catch (Exception e) {
            System.out.println("Erro na operação.");
        }
    }

    private void excluir() {
        System.out.print("Digite o ID do livro para excluir: ");
        try {
            int id = Integer.parseInt(console.nextLine());
            if (controller.excluir(id)) {
                System.out.println("Livro excluído logicamente com sucesso!");
            } else {
                System.out.println("Livro não encontrado ou já excluído.");
            }
        } catch (Exception e) {
            System.out.println("Erro ao excluir.");
        }
    }
}