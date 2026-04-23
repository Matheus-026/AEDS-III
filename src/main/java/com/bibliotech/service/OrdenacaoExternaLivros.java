package com.bibliotech.service;

import org.springframework.stereotype.Service;
import com.bibliotech.model.Livro;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OrdenacaoExternaLivros {

    private final String ARQUIVO_PRINCIPAL = "data/livros.dat";
    // Simulando uma memória RAM muito pequena (cabe apenas 10 livros por vez)
    // Isso é ideal para o professor ver os blocos sendo formados no seu vídeo demonstrativo!
    private final int TAMANHO_MEMORIA_RAM = 10; 

    public void ordenarPorPreco() throws Exception {
        System.out.println("Iniciando Ordenação Externa...");
        
        // Passo 1: Geração dos blocos iniciais ordenados
        int totalBlocos = faseDeDistribuicao();
        
        // Passo 2: Intercalação
        if (totalBlocos > 1) {
            faseDeIntercalacao(totalBlocos);
        } else {
            System.out.println("O arquivo principal já coube inteiro na RAM. Ordenação concluída.");
        }
    }

    /**
     * FASE 1: Lê o arquivo original, cria blocos do tamanho da RAM, ordena e distribui.
     */
    private int faseDeDistribuicao() throws Exception {
        RandomAccessFile arqPrincipal = new RandomAccessFile(ARQUIVO_PRINCIPAL, "r");
        
        // Criando os arquivos temporários de entrada
        RandomAccessFile temp1 = new RandomAccessFile("data/temp1.dat", "rw");
        RandomAccessFile temp2 = new RandomAccessFile("data/temp2.dat", "rw");
        
        temp1.setLength(0); // Limpa arquivos antigos
        temp2.setLength(0);

        List<Livro> blocoMemoria = new ArrayList<>();
        boolean alternarArquivo = true;
        int contBlocos = 0;

        // Pula o cabeçalho (últimoID e quantidade)
        if(arqPrincipal.length() > 0) {
            arqPrincipal.seek(8); 
        }

        while (arqPrincipal.getFilePointer() < arqPrincipal.length()) {
            boolean ativo = arqPrincipal.readBoolean();
            int tamanhoRegistro = arqPrincipal.readInt();
            byte[] dados = new byte[tamanhoRegistro];
            arqPrincipal.readFully(dados);

            // Só carregamos para a RAM se não estiver excluído (lapide == true)
            if (ativo) {
                Livro l = new Livro();
                l.fromByteArray(dados);
                blocoMemoria.add(l);
            }

            // Se a "RAM" encheu ou o arquivo acabou
            if (blocoMemoria.size() == TAMANHO_MEMORIA_RAM || arqPrincipal.getFilePointer() == arqPrincipal.length()) {
                if (!blocoMemoria.isEmpty()) {
                    // Ordena o bloco na RAM usando o preço
                    blocoMemoria.sort(Comparator.comparing(Livro::getPreco));

                    // Grava no arquivo temporário da vez
                    RandomAccessFile tempDestino = alternarArquivo ? temp1 : temp2;
                    gravarBloco(tempDestino, blocoMemoria);

                    alternarArquivo = !alternarArquivo; // Troca o arquivo destino
                    blocoMemoria.clear(); // Esvazia a RAM
                    contBlocos++;
                }
            }
        }

        arqPrincipal.close();
        temp1.close();
        temp2.close();
        
        System.out.println("Fase de Distribuição concluída. Foram gerados " + contBlocos + " blocos.");
        return contBlocos;
    }

    /**
     * Método auxiliar para gravar os bytes ordenados no arquivo temporário.
     * Não gravamos lápide aqui, pois só trouxemos os livros ativos!
     */
    private void gravarBloco(RandomAccessFile tempDestino, List<Livro> bloco) throws IOException {
        for (Livro l : bloco) {
            byte[] dados = l.toByteArray();
            tempDestino.writeInt(dados.length);
            tempDestino.write(dados);
        }
    }

    /**
     * FASE 2: Intercalação Balanceada
     */
    private void faseDeIntercalacao(int totalBlocos) throws Exception {
        int tamanhoBlocoAtual = TAMANHO_MEMORIA_RAM;
        boolean lendoDosTemps1e2 = true; // Controla o ping-pong (1 e 2) -> (3 e 4)

        while (totalBlocos > 1) {
            String in1 = lendoDosTemps1e2 ? "data/temp1.dat" : "data/temp3.dat";
            String in2 = lendoDosTemps1e2 ? "data/temp2.dat" : "data/temp4.dat";
            String out1 = lendoDosTemps1e2 ? "data/temp3.dat" : "data/temp1.dat";
            String out2 = lendoDosTemps1e2 ? "data/temp4.dat" : "data/temp2.dat";

            RandomAccessFile arqIn1 = new RandomAccessFile(in1, "r");
            RandomAccessFile arqIn2 = new RandomAccessFile(in2, "r");
            RandomAccessFile arqOut1 = new RandomAccessFile(out1, "rw");
            RandomAccessFile arqOut2 = new RandomAccessFile(out2, "rw");

            arqOut1.setLength(0); // Limpa os arquivos de destino para esta rodada
            arqOut2.setLength(0);

            boolean alternarSaida = true;

            // Enquanto houver dados em qualquer um dos arquivos de entrada
            while (arqIn1.getFilePointer() < arqIn1.length() || arqIn2.getFilePointer() < arqIn2.length()) {
                RandomAccessFile arqDestino = alternarSaida ? arqOut1 : arqOut2;
                intercalarBlocos(arqIn1, arqIn2, arqDestino, tamanhoBlocoAtual);
                alternarSaida = !alternarSaida;
            }

            arqIn1.close();
            arqIn2.close();
            arqOut1.close();
            arqOut2.close();

            tamanhoBlocoAtual *= 2; // Dobra o tamanho do bloco para a próxima passada
            totalBlocos = (totalBlocos + 1) / 2; // Arredonda para cima
            lendoDosTemps1e2 = !lendoDosTemps1e2; // Inverte os papéis
        }

        // Ao final, o arquivo ordenado será o temp1 ou temp3
        String arquivoFinal = lendoDosTemps1e2 ? "data/temp1.dat" : "data/temp3.dat";
        substituirArquivoPrincipal(arquivoFinal);
    }

    /**
     * Compara e funde dois blocos gravando no arquivo de destino
     */
    private void intercalarBlocos(RandomAccessFile in1, RandomAccessFile in2, RandomAccessFile out, int tamanhoBloco) throws Exception {
        int lidos1 = 0, lidos2 = 0;
        long pos1 = in1.getFilePointer();
        long pos2 = in2.getFilePointer();
        Livro l1 = lerLivro(in1);
        Livro l2 = lerLivro(in2);

        while ((l1 != null && lidos1 < tamanhoBloco) || (l2 != null && lidos2 < tamanhoBloco)) {
            boolean usarL1 = false;
            
            if (l1 != null && lidos1 < tamanhoBloco) {
                if (l2 == null || lidos2 >= tamanhoBloco || l1.getPreco() <= l2.getPreco()) {
                    usarL1 = true;
                }
            }

            if (usarL1) {
                gravarLivro(out, l1);
                lidos1++;
                pos1 = in1.getFilePointer();
                if (lidos1 < tamanhoBloco) l1 = lerLivro(in1);
            } else {
                gravarLivro(out, l2);
                lidos2++;
                pos2 = in2.getFilePointer();
                if (lidos2 < tamanhoBloco) l2 = lerLivro(in2);
            }
        }

        // Devolve o ponteiro se um livro foi lido mas não consumido nesta rodada
        if (l1 != null && lidos1 >= tamanhoBloco) in1.seek(pos1);
        if (l2 != null && lidos2 >= tamanhoBloco) in2.seek(pos2);
    }

    private Livro lerLivro(RandomAccessFile arq) throws IOException {
        if (arq.getFilePointer() >= arq.length()) return null;
        int tamanho = arq.readInt();
        byte[] dados = new byte[tamanho];
        arq.readFully(dados);
        Livro l = new Livro();
        l.fromByteArray(dados);
        return l;
    }
    
    private void gravarLivro(RandomAccessFile arq, Livro l) throws IOException {
        byte[] dados = l.toByteArray();
        arq.writeInt(dados.length);
        arq.write(dados);
    }

    /**
     * Transfere os dados ordenados de volta para o livros.dat e apaga a Hash antiga
     */
    private void substituirArquivoPrincipal(String arquivoFinal) throws Exception {
        RandomAccessFile arqOriginal = new RandomAccessFile(ARQUIVO_PRINCIPAL, "rw");
        RandomAccessFile arqOrdenado = new RandomAccessFile(arquivoFinal, "r");

        // Preserva o cabeçalho original
        arqOriginal.seek(0);
        int ultimoID = 0, quantidade = 0;
        if (arqOriginal.length() >= 8) {
            ultimoID = arqOriginal.readInt();
            quantidade = arqOriginal.readInt();
        }

        arqOriginal.setLength(0); // Limpa o original
        arqOriginal.writeInt(ultimoID);
        arqOriginal.writeInt(quantidade);

        // Copia os dados recriando a lápide de registro ativo (boolean true)
        while (arqOrdenado.getFilePointer() < arqOrdenado.length()) {
            Livro l = lerLivro(arqOrdenado);
            if (l != null) {
                byte[] dados = l.toByteArray();
                arqOriginal.writeBoolean(true); // LÁPIDE (Registro Ativo)
                arqOriginal.writeInt(dados.length);
                arqOriginal.write(dados);
            }
        }

        arqOriginal.close();
        arqOrdenado.close();
        
        // DICA DE OURO: Como mudamos os livros de lugar físico, a tabela Hash antiga não serve mais!
        // Deletamos os arquivos da Hash. Quando o Spring Boot ligar, o seu LivroDAO vai perceber 
        // que eles sumiram e vai chamar a função reconstruirHash() automaticamente!
        new File("data/diretorios/livros_dir.hash").delete();
        new File("data/buckets/livros_bucket.hash").delete();

        System.out.println("SUCESSO! Arquivo principal foi ordenado por preço.");
    }
}