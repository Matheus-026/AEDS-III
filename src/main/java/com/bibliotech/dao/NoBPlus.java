package com.bibliotech.dao;

import java.io.*;

public class NoBPlus {
    private int ordem; //m = 5
    private int numChaves; // Quantidade de títulos atualmente neste nó
    private String[] chaves; // Os Títulos (limitados a 30 caracteres)
    private long[] valores; // Posição física no livros.dat (apenas para nós folha)
    private long[] filhos; // Posições no arquivo da árvore (apenas para nós internos)
    private boolean folha; // Verdadeiro se for a última camada (folha)
    private long proximo; // Ponteiro para a próxima folha (a mágica para a listagem em ordem!)

    private final int TAMANHO_MAX_STRING = 30;

    // Construtor
    public NoBPlus(int ordem, boolean folha) {
        this.ordem = ordem;
        this.folha = folha;
        this.numChaves = 0;

        this.chaves = new String[ordem]; 
        this.valores = new long[ordem];
        this.filhos = new long[ordem + 1]; 
        this.proximo = -1;
        
        // Inicializando os arrays com valores vazios
        for (int i = 0; i < chaves.length; i++) chaves[i] = "";
        for (int i = 0; i < valores.length; i++) valores[i] = -1;
        for (int i = 0; i < filhos.length; i++) filhos[i] = -1;
    }

    // ==========================================
    // SERIALIZAÇÃO (Gravar no Disco)
    // ==========================================
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(numChaves);
        dos.writeBoolean(folha);

        // Escreve as chaves garantindo tamanho fixo de 30 caracteres
        for (int i = 0; i < ordem - 1; i++) {
            String chaveFixa = chaves[i];
            if (chaveFixa == null) chaveFixa = "";
            
            if (chaveFixa.length() > TAMANHO_MAX_STRING) {
                chaveFixa = chaveFixa.substring(0, TAMANHO_MAX_STRING);
            } else {
                // Preenche com espaços em branco à direita se for menor
                chaveFixa = String.format("%-" + TAMANHO_MAX_STRING + "s", chaveFixa); 
            }
            dos.writeUTF(chaveFixa);
        }

        // Escreve os valores (ponteiros de dados)
        for (int i = 0; i < ordem - 1; i++) dos.writeLong(valores[i]);
        
        // Escreve os filhos (ponteiros de nós)
        for (int i = 0; i < ordem; i++) dos.writeLong(filhos[i]);

        // Ponteiro para o próximo nó folha
        dos.writeLong(proximo);

        return baos.toByteArray();
    }

    // ==========================================
    // DESERIALIZAÇÃO (Ler do Disco)
    // ==========================================
    public void fromByteArray(byte[] buffer) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        DataInputStream dis = new DataInputStream(bais);

        numChaves = dis.readInt();
        folha = dis.readBoolean();

        // O .trim() remove os espaços em branco extras que colocamos na gravação
        for (int i = 0; i < ordem - 1; i++) chaves[i] = dis.readUTF().trim();
        for (int i = 0; i < ordem - 1; i++) valores[i] = dis.readLong();
        for (int i = 0; i < ordem; i++) filhos[i] = dis.readLong();
        
        proximo = dis.readLong();
    }

    // ==========================================
    // GETTERS E SETTERS 
    // ==========================================
    public int getNumChaves() { return numChaves; }
    public void setNumChaves(int numChaves) { this.numChaves = numChaves; }
    public String getChave(int i) { return chaves[i]; }
    public void setChave(int i, String chave) { this.chaves[i] = chave; }
    public long getValor(int i) { return valores[i]; }
    public void setValor(int i, long valor) { this.valores[i] = valor; }
    public long getFilho(int i) { return filhos[i]; }
    public void setFilho(int i, long filho) { this.filhos[i] = filho; }
    public boolean isFolha() { return folha; }
    public void setFolha(boolean folha) { this.folha = folha; }
    public long getProximo() { return proximo; }
    public void setProximo(long proximo) { this.proximo = proximo; }
}