package com.bibliotech.dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ArvoreBPlus {
    private int ordem;
    private RandomAccessFile arquivo;
    private String nomeArquivo;
    private long raiz; // Endereço físico da raiz no arquivo
    
    // Tamanho fixo do bloco em bytes. 
    // Garante que cada nó ocupe o mesmo espaço no disco, permitindo o seek().
    private final int TAMANHO_BLOCO = 300; 

    public ArvoreBPlus(int ordem, String nomeArquivo) throws IOException {
        this.ordem = ordem;
        this.nomeArquivo = "data/indices/" + nomeArquivo + ".bplus";
        
        // Cria a pasta de índices se não existir
        File dir = new File("data/indices");
        if (!dir.exists()) dir.mkdirs();

        this.arquivo = new RandomAccessFile(this.nomeArquivo, "rw");

        if (this.arquivo.length() < 8) {
            // Arquivo novo: cria a raiz (que inicialmente é uma folha) e grava no cabeçalho
            this.raiz = 8; // A raiz começa logo após o cabeçalho (que tem 8 bytes)
            this.arquivo.seek(0);
            this.arquivo.writeLong(this.raiz);
            
            NoBPlus noRaiz = new NoBPlus(ordem, true);
            escreverNo(this.raiz, noRaiz);
        } else {
            // Arquivo já existe: lê o endereço da raiz
            this.arquivo.seek(0);
            this.raiz = this.arquivo.readLong();
        }
    }

    // ==========================================
    // LEITURA E GRAVAÇÃO DE NÓS
    // ==========================================
    private void escreverNo(long endereco, NoBPlus no) throws IOException {
        arquivo.seek(endereco);
        byte[] dados = no.toByteArray();
        byte[] bloco = new byte[TAMANHO_BLOCO]; 
        
        // Copia os dados do nó para um bloco de tamanho fixo 
        // (preenche o resto com zeros se sobrar espaço)
        System.arraycopy(dados, 0, bloco, 0, Math.min(dados.length, TAMANHO_BLOCO));
        arquivo.write(bloco);
    }

    private NoBPlus lerNo(long endereco) throws IOException {
        arquivo.seek(endereco);
        byte[] bloco = new byte[TAMANHO_BLOCO];
        arquivo.readFully(bloco);
        
        NoBPlus no = new NoBPlus(this.ordem, true);
        no.fromByteArray(bloco);
        return no;
    }

    // ==========================================
    // CONSULTA ORDENADA
    // ==========================================
    public List<Long> listarEmOrdemAlfabetica() throws IOException {
        List<Long> posicoesFisicas = new ArrayList<>();
        
        // 1. Desce até a primeira folha (a mais à esquerda da árvore)
        long enderecoAtual = raiz;
        NoBPlus noAtual = lerNo(enderecoAtual);
        
        while (!noAtual.isFolha()) {
            enderecoAtual = noAtual.getFilho(0); // Vai sempre para o filho 0 (menores valores)
            noAtual = lerNo(enderecoAtual);
        }
        
        // 2. Navega pelas folhas usando a lista encadeada (ponteiro "proximo")
        while (enderecoAtual != -1) {
            for (int i = 0; i < noAtual.getNumChaves(); i++) {
                // Adicionamos a posição física do livro que está gravada na folha
                posicoesFisicas.add(noAtual.getValor(i)); 
            }
            
            // Salta para o próximo nó folha no disco
            enderecoAtual = noAtual.getProximo();
            if (enderecoAtual != -1) {
                noAtual = lerNo(enderecoAtual);
            }
        }
        
        // Retorna as posições prontas para o DAO ir buscar no livros.dat
        return posicoesFisicas;
    }

    // ==========================================
    // INSERÇÃO E BALANCEAMENTO (SPLIT)
    // ==========================================
    
    // Classe auxiliar para subir a chave e o ponteiro quando um nó "explode"
    private class RetornoSplit {
        public String chavePromovida;
        public long enderecoFilhoDireito;
        public RetornoSplit(String chave, long filhoDireito) {
            this.chavePromovida = chave;
            this.enderecoFilhoDireito = filhoDireito;
        }
    }

     //Ponto de entrada público para inserir um novo livro na árvore
   
    public void inserir(String chave, long valor) throws IOException {
        RetornoSplit splitDaRaiz = inserirRecursivo(raiz, chave, valor);

        // Se a raiz se dividiu, a árvore cresce um nível para cima
        // Precisa criar uma NOVA raiz apontando para as duas metades.
        if (splitDaRaiz != null) {
            NoBPlus novaRaiz = new NoBPlus(ordem, false);
            novaRaiz.setChave(0, splitDaRaiz.chavePromovida);
            novaRaiz.setFilho(0, raiz);
            novaRaiz.setFilho(1, splitDaRaiz.enderecoFilhoDireito);
            novaRaiz.setNumChaves(1);

            long enderecoNovaRaiz = arquivo.length();
            escreverNo(enderecoNovaRaiz, novaRaiz);

            // Atualiza o cabeçalho do arquivo .bplus com a nova raiz
            raiz = enderecoNovaRaiz;
            arquivo.seek(0);
            arquivo.writeLong(raiz);
        }
    }

    private RetornoSplit inserirRecursivo(long enderecoAtual, String chave, long valor) throws IOException {
        NoBPlus noAtual = lerNo(enderecoAtual);

        if (noAtual.isFolha()) {
            // CASO BASE: Chegámos à folha, tenta inserir aqui
            return inserirNaFolha(enderecoAtual, noAtual, chave, valor);
        } else {
            // NAVEGAÇÃO: Procura qual filho deve receber a nova chave
            int i = 0;
            while (i < noAtual.getNumChaves() && chave.compareToIgnoreCase(noAtual.getChave(i)) > 0) {
                i++;
            }

            RetornoSplit splitFilho = inserirRecursivo(noAtual.getFilho(i), chave, valor);

            // Se o filho explodiu, este nó interno precisa receber a chave promovida
            if (splitFilho != null) {
                return inserirNoInterno(enderecoAtual, noAtual, splitFilho.chavePromovida, splitFilho.enderecoFilhoDireito);
            }
        }
        return null; // O filho absorveu a inserção sem problemas
    }

    private RetornoSplit inserirNaFolha(long enderecoAtual, NoBPlus folha, String chave, long valor) throws IOException {
        // Desloca as chaves para a direita para manter a ordem alfabética no nó
        int i = folha.getNumChaves() - 1;
        while (i >= 0 && chave.compareToIgnoreCase(folha.getChave(i)) < 0) {
            folha.setChave(i + 1, folha.getChave(i));
            folha.setValor(i + 1, folha.getValor(i));
            i--;
        }
        folha.setChave(i + 1, chave);
        folha.setValor(i + 1, valor);
        folha.setNumChaves(folha.getNumChaves() + 1);

        // Se ainda há espaço na folha (Ordem - 1), basta guardar no disco e terminar
        if (folha.getNumChaves() < ordem) {
            escreverNo(enderecoAtual, folha);
            return null;
        }

        //Iniciar o Split da Folha!
        return splitFolha(enderecoAtual, folha);
    }

    private RetornoSplit inserirNoInterno(long enderecoAtual, NoBPlus interno, String chave, long filhoDireito) throws IOException {
        int i = interno.getNumChaves() - 1;
        while (i >= 0 && chave.compareToIgnoreCase(interno.getChave(i)) < 0) {
            interno.setChave(i + 1, interno.getChave(i));
            interno.setFilho(i + 2, interno.getFilho(i + 1));
            i--;
        }
        interno.setChave(i + 1, chave);
        interno.setFilho(i + 2, filhoDireito);
        interno.setNumChaves(interno.getNumChaves() + 1);

        if (interno.getNumChaves() < ordem) {
            escreverNo(enderecoAtual, interno);
            return null;
        }

        //Iniciar o Split do Nó Interno
        return splitNoInterno(enderecoAtual, interno);
    }

    private RetornoSplit splitFolha(long enderecoAtual, NoBPlus folhaAntiga) throws IOException {
        NoBPlus novaFolha = new NoBPlus(ordem, true);
        int meio = (ordem) / 2;

        // Move a metade superior das chaves para a folha nova
        for (int i = meio; i < ordem; i++) {
            novaFolha.setChave(i - meio, folhaAntiga.getChave(i));
            novaFolha.setValor(i - meio, folhaAntiga.getValor(i));
            novaFolha.setNumChaves(novaFolha.getNumChaves() + 1);
            
            // Limpa a folha antiga
            folhaAntiga.setChave(i, "");
            folhaAntiga.setValor(i, -1);
        }
        folhaAntiga.setNumChaves(meio);

        //Atualiza os ponteiros da lista encadeada entre as folhas
        novaFolha.setProximo(folhaAntiga.getProximo());
        long enderecoNovaFolha = arquivo.length();
        folhaAntiga.setProximo(enderecoNovaFolha);

        escreverNo(enderecoAtual, folhaAntiga);
        escreverNo(enderecoNovaFolha, novaFolha);

        // Numa B+, a chave promovida da folha É COPIADA para o pai, permanecendo também na folha
        return new RetornoSplit(novaFolha.getChave(0), enderecoNovaFolha);
    }

    private RetornoSplit splitNoInterno(long enderecoAtual, NoBPlus internoAntigo) throws IOException {
        NoBPlus novoInterno = new NoBPlus(ordem, false);
        int meio = (ordem) / 2;
        String chavePromovida = internoAntigo.getChave(meio);

        // Move a metade superior para o novo nó interno
        int j = 0;
        for (int i = meio + 1; i < ordem; i++) {
            novoInterno.setChave(j, internoAntigo.getChave(i));
            novoInterno.setFilho(j, internoAntigo.getFilho(i));
            novoInterno.setNumChaves(novoInterno.getNumChaves() + 1);
            
            internoAntigo.setChave(i, "");
            internoAntigo.setFilho(i, -1);
            j++;
        }
        
        // Move o último filho que fica na borda
        novoInterno.setFilho(j, internoAntigo.getFilho(ordem));
        internoAntigo.setFilho(ordem, -1);

        // Limpa a chave do meio na folha antiga
        internoAntigo.setChave(meio, "");
        internoAntigo.setNumChaves(meio);

        long enderecoNovoInterno = arquivo.length();
        escreverNo(enderecoAtual, internoAntigo);
        escreverNo(enderecoNovoInterno, novoInterno);

        return new RetornoSplit(chavePromovida, enderecoNovoInterno);
    }
}