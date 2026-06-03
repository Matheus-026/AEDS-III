package com.bibliotech.compression;

public class HuffmanNode {

    byte dado;
    int frequencia;

    HuffmanNode esquerda;
    HuffmanNode direita;

    public HuffmanNode(byte dado, int frequencia) {
        this.dado = dado;
        this.frequencia = frequencia;
    }

    public HuffmanNode(
            int frequencia,
            HuffmanNode esquerda,
            HuffmanNode direita) {

        this.frequencia = frequencia;
        this.esquerda = esquerda;
        this.direita = direita;
    }
}