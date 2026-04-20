package com.bibliotech.dao;

import java.io.*;

public class HashExtensivel {

    private RandomAccessFile diretorio;
    private RandomAccessFile buckets;

    private int profundidadeGlobal;
    private final int TAM_BUCKET = 4;

    public HashExtensivel(String nome) throws IOException {

        diretorio = new RandomAccessFile("data/diretorios/" + nome + "_dir.hash", "rw");
        buckets   = new RandomAccessFile("data/buckets/" + nome + "_bucket.hash", "rw");

        if (diretorio.length() == 0) {

            profundidadeGlobal = 1;
            diretorio.writeInt(profundidadeGlobal);

            long b0 = criarBucket(1);

            // 🔥 ambos apontam pro mesmo bucket
            diretorio.writeLong(b0);
            diretorio.writeLong(b0);

        } else {
            diretorio.seek(0);
            profundidadeGlobal = diretorio.readInt();
        }
    }

    // =========================
    // HASH
    // =========================
    private int hash(int id) {
        return id & ((1 << profundidadeGlobal) - 1);
    }

    // =========================
    // CRIAR BUCKET
    // =========================
    private long criarBucket(int profundidadeLocal) throws IOException {

        long pos = buckets.length();
        buckets.seek(pos);

        buckets.writeInt(0); // qtd
        buckets.writeInt(profundidadeLocal);

        for (int i = 0; i < TAM_BUCKET; i++) {
            buckets.writeInt(-1);
            buckets.writeLong(-1);
        }

        return pos;
    }

    // =========================
    // INSERIR
    // =========================
    public void inserir(int id, long posArquivo) throws IOException {

        int h = hash(id);
        long posBucket = getEnderecoBucket(h);

        buckets.seek(posBucket);

        int qtd = buckets.readInt();
        int pl  = buckets.readInt();

        if (qtd < TAM_BUCKET) {

            buckets.seek(posBucket + 8 + qtd * 12);
            buckets.writeInt(id);
            buckets.writeLong(posArquivo);

            buckets.seek(posBucket);
            buckets.writeInt(qtd + 1);

        } else {
            splitBucket(h, posBucket, pl);
            inserir(id, posArquivo);
        }
    }

    // =========================
    // BUSCAR
    // =========================
    public long buscar(int id) throws IOException {

        int h = hash(id);
        long posBucket = getEnderecoBucket(h);

        buckets.seek(posBucket);

        int qtd = buckets.readInt();
        buckets.readInt();

        for (int i = 0; i < qtd; i++) {
            int idLido = buckets.readInt();
            long pos   = buckets.readLong();

            if (idLido == id) return pos;
        }

        return -1;
    }

    // =========================
    // REMOVER
    // =========================
    public void remover(int id) throws IOException {

        int h = hash(id);
        long posBucket = getEnderecoBucket(h);

        buckets.seek(posBucket);

        int qtd = buckets.readInt();
        buckets.readInt();

        for (int i = 0; i < qtd; i++) {

            long posAtual = buckets.getFilePointer();

            int idLido = buckets.readInt();
            long pos   = buckets.readLong();

            if (idLido == id) {

                long fim = posBucket + 8 + (qtd - 1) * 12;

                buckets.seek(fim);
                int lastId = buckets.readInt();
                long lastPos = buckets.readLong();

                buckets.seek(posAtual);
                buckets.writeInt(lastId);
                buckets.writeLong(lastPos);

                buckets.seek(posBucket);
                buckets.writeInt(qtd - 1);

                return;
            }
        }
    }

    // =========================
    // SPLIT
    // =========================
    private void splitBucket(int hash, long posBucket, int pl) throws IOException {

        if (pl == profundidadeGlobal) {
            duplicarDiretorio();
        }

        long novoBucket = criarBucket(pl + 1);

        // atualiza profundidade local do bucket antigo
        buckets.seek(posBucket + 4);
        buckets.writeInt(pl + 1);

        int tamanhoDir = (int) Math.pow(2, profundidadeGlobal);

        for (int i = 0; i < tamanhoDir; i++) {

            diretorio.seek(4 + i * 8);
            long endereco = diretorio.readLong();

            if (endereco == posBucket) {

                // 🔥 usa o bit da profundidade local
                if (((i >> pl) & 1) == 1) {
                    diretorio.seek(4 + i * 8);
                    diretorio.writeLong(novoBucket);
                }
            }
        }

        redistribuir(posBucket);
    }

    // =========================
    // DUPLICAR DIRETÓRIO
    // =========================
    private void duplicarDiretorio() throws IOException {

        int tamanho = (int) Math.pow(2, profundidadeGlobal);

        long[] antigos = new long[tamanho];

        diretorio.seek(4);

        for (int i = 0; i < tamanho; i++) {
            antigos[i] = diretorio.readLong();
        }

        diretorio.setLength(0);

        profundidadeGlobal++;

        diretorio.writeInt(profundidadeGlobal);

        for (int i = 0; i < tamanho; i++) {
            diretorio.writeLong(antigos[i]);
        }

        for (int i = 0; i < tamanho; i++) {
            diretorio.writeLong(antigos[i]);
        }
    }

    // =========================
    // REDISTRIBUIR
    // =========================
    private void redistribuir(long posBucket) throws IOException {

        buckets.seek(posBucket);

        int qtd = buckets.readInt();
        int pl  = buckets.readInt();

        int[] ids = new int[qtd];
        long[] pos = new long[qtd];

        for (int i = 0; i < qtd; i++) {
            ids[i] = buckets.readInt();
            pos[i] = buckets.readLong();
        }

        // 🔥 limpa bucket antigo
        buckets.seek(posBucket);
        buckets.writeInt(0);

        // 🔥 MUITO IMPORTANTE:
        // reinserir usando o método NORMAL
        for (int i = 0; i < qtd; i++) {
            inserir(ids[i], pos[i]);
        }
    }

    // =========================
    // GET BUCKET
    // =========================
    private long getEnderecoBucket(int hash) throws IOException {
        diretorio.seek(4 + hash * 8);
        return diretorio.readLong();
    }
}