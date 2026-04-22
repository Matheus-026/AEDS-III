package com.bibliotech.dao;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class HashExtensivel {

    private RandomAccessFile diretorio;
    private RandomAccessFile buckets;

    private int profundidadeGlobal;
    private final int TAM_BUCKET = 10;

    // Tamanho fixo de cada slot dentro do bucket:
    // 4 (chave) + 4 (qtdLista) + TAM_BUCKET * 8 (valores long)
    private final int TAM_SLOT = 4 + 4 + TAM_BUCKET * 8;

    // Offset do primeiro slot dentro do bucket (qtd=4 + pl=4)
    private final int OFFSET_SLOTS = 8;

    public HashExtensivel(String nome) throws IOException {

        diretorio = new RandomAccessFile("data/diretorios/" + nome + "_dir.hash", "rw");
        buckets   = new RandomAccessFile("data/buckets/" + nome + "_bucket.hash", "rw");

        if (diretorio.length() == 0) {

            profundidadeGlobal = 1;
            diretorio.writeInt(profundidadeGlobal);

            long b0 = criarBucket(1);

            diretorio.writeLong(b0);
            diretorio.writeLong(b0);

        } else {
            diretorio.seek(0);
            profundidadeGlobal = diretorio.readInt();
        }
    }

    private int hash(int chave) {
        return chave & ((1 << profundidadeGlobal) - 1);
    }

    private long criarBucket(int pl) throws IOException {

        long pos = buckets.length();
        buckets.seek(pos);

        buckets.writeInt(0);  // qtd registros
        buckets.writeInt(pl); // profundidade local

        for (int i = 0; i < TAM_BUCKET; i++) {
            buckets.writeInt(-1); // chave vazia
            buckets.writeInt(0);  // qtd lista
            for (int j = 0; j < TAM_BUCKET; j++) {
                buckets.writeLong(-1L); // valores vazios
            }
        }

        return pos;
    }

    private long getEnderecoBucket(int h) throws IOException {
        diretorio.seek(4 + (long) h * 8);
        return diretorio.readLong();
    }

    private void duplicarDiretorio() throws IOException {

        int tamanho = 1 << profundidadeGlobal;
        List<Long> lista = new ArrayList<>();

        diretorio.seek(4);
        for (int i = 0; i < tamanho; i++) {
            lista.add(diretorio.readLong());
        }

        diretorio.setLength(0);
        profundidadeGlobal++;
        diretorio.writeInt(profundidadeGlobal);

        for (long l : lista) diretorio.writeLong(l);
        for (long l : lista) diretorio.writeLong(l);
    }

  
    // INSERIR (PK ou 1:N)
    public void inserirLista(int chave, long valor) throws IOException {

        while (true) {

            int h = hash(chave);
            long posBucket = getEnderecoBucket(h);

            buckets.seek(posBucket);
            int qtd = buckets.readInt();
            int pl  = buckets.readInt();

            // procura chave já existente no bucket
            for (int i = 0; i < TAM_BUCKET; i++) {

                long posRegistro = posBucket + OFFSET_SLOTS + (long) i * TAM_SLOT;
                buckets.seek(posRegistro);

                int k        = buckets.readInt();
                int qtdLista = buckets.readInt();

                if (k == chave) {
                    if (qtdLista >= TAM_BUCKET) {
                        // lista cheia para esta chave — não cabe mais valores
                        // (limitação do TAM_BUCKET fixo)
                        throw new IOException(
                            "Lista cheia para chave=" + chave +
                            ". Aumente TAM_BUCKET.");
                    }
                    // adiciona no próximo slot da lista
                    buckets.seek(posRegistro + 8 + (long) qtdLista * 8);
                    buckets.writeLong(valor);

                    // atualiza qtdLista
                    buckets.seek(posRegistro + 4);
                    buckets.writeInt(qtdLista + 1);

                    return;
                }
            }

            //  espaço livre para nova chave 
            if (qtd < TAM_BUCKET) {

                for (int i = 0; i < TAM_BUCKET; i++) {

                    long posRegistro = posBucket + OFFSET_SLOTS + (long) i * TAM_SLOT;
                    buckets.seek(posRegistro);

                    int k = buckets.readInt();

                    if (k == -1) {
                        buckets.seek(posRegistro);
                        buckets.writeInt(chave);
                        buckets.writeInt(1);
                        buckets.writeLong(valor);

                        // atualiza qtd do bucket
                        buckets.seek(posBucket);
                        buckets.writeInt(qtd + 1);

                        return;
                    }
                }
            }

            // ── bucket cheio: SPLIT ───────────────────────────────────────────
            if (pl == profundidadeGlobal) {
                duplicarDiretorio();
                continue; // recalcula hash com nova profundidade
            }

            long novoBucket = criarBucket(pl + 1);

            // atualiza profundidade local do bucket antigo
            buckets.seek(posBucket + 4);
            buckets.writeInt(pl + 1);

            redistribuir(posBucket, novoBucket, pl);
            // não retorna: tenta inserir novamente
        }
    }


    // REDISTRIBUIR (corrigido)
    private void redistribuir(long antigo, long novo, int pl) throws IOException {

        // Lê todos os pares (chave, valores) do bucket antigo
        List<Integer>    chaves  = new ArrayList<>();
        List<List<Long>> valores = new ArrayList<>();

        for (int i = 0; i < TAM_BUCKET; i++) {

            long posRegistro = antigo + OFFSET_SLOTS + (long) i * TAM_SLOT;
            buckets.seek(posRegistro);

            int k        = buckets.readInt();
            int qtdLista = buckets.readInt();

            if (k != -1) {
                List<Long> lista = new ArrayList<>();
                for (int j = 0; j < qtdLista; j++) {
                    lista.add(buckets.readLong());
                }
                chaves.add(k);
                valores.add(lista);
            }
        }

        // Zera o bucket antigo corretamente
        //    (qtd=0, mantém pl que já foi atualizado, slots todos -1)
        buckets.seek(antigo);
        buckets.writeInt(0); // qtd = 0
        // pula os 4 bytes do pl (já atualizado antes de chamar redistribuir)
        buckets.seek(antigo + OFFSET_SLOTS);
        for (int i = 0; i < TAM_BUCKET; i++) {
            buckets.writeInt(-1); // chave
            buckets.writeInt(0);  // qtdLista
            for (int j = 0; j < TAM_BUCKET; j++) {
                buckets.writeLong(-1L);
            }
        }

        // Atualiza ponteiros do diretório para o novo bucket
        int dirSize = 1 << profundidadeGlobal;

        for (int i = 0; i < dirSize; i++) {
            long addr = getEnderecoBucket(i);
            if (addr == antigo && ((i >> pl) & 1) == 1) {
                diretorio.seek(4 + (long) i * 8);
                diretorio.writeLong(novo);
            }
        }

        // Reinsere todos os registros
        for (int i = 0; i < chaves.size(); i++) {
            for (long v : valores.get(i)) {
                inserirLista(chaves.get(i), v);
            }
        }
    }


    // BUSCAR LISTA
    public List<Long> buscarLista(int chave) throws IOException {

        List<Long> lista = new ArrayList<>();

        int h = hash(chave);
        long posBucket = getEnderecoBucket(h);

        for (int i = 0; i < TAM_BUCKET; i++) {

            long posRegistro = posBucket + OFFSET_SLOTS + (long) i * TAM_SLOT;
            buckets.seek(posRegistro);

            int k   = buckets.readInt();
            int qtd = buckets.readInt();

            if (k == chave) {
                for (int j = 0; j < qtd; j++) {
                    lista.add(buckets.readLong());
                }
                return lista;
            }
        }

        return lista; // vazia se não encontrado
    }

    // REMOVER ENTRADA INTEIRA (PK)
    // Apaga a entrada completa do bucket e decrementa qtd.
    public void remover(int chave) throws IOException {

        int h = hash(chave);
        long posBucket = getEnderecoBucket(h);

        buckets.seek(posBucket);
        int qtd = buckets.readInt();

        for (int i = 0; i < TAM_BUCKET; i++) {

            long posRegistro = posBucket + OFFSET_SLOTS + (long) i * TAM_SLOT;
            buckets.seek(posRegistro);

            int k = buckets.readInt();

            if (k == chave) {
                // apaga a entrada: chave=-1, qtd=0, valores=-1
                buckets.seek(posRegistro);
                buckets.writeInt(-1);
                buckets.writeInt(0);
                for (int j = 0; j < TAM_BUCKET; j++) {
                    buckets.writeLong(-1L);
                }

                // decrementa qtd do bucket
                buckets.seek(posBucket);
                buckets.writeInt(qtd - 1);
                return;
            }
        }
    }


    // REMOVER UM VALOR DA LISTA (1:N)
    // Remove apenas o valor específico da lista de uma chave.
    public void removerLista(int chave, long valor) throws IOException {

        int h = hash(chave);
        long posBucket = getEnderecoBucket(h);

        for (int i = 0; i < TAM_BUCKET; i++) {

            long posRegistro = posBucket + OFFSET_SLOTS + (long) i * TAM_SLOT;
            buckets.seek(posRegistro);

            int k   = buckets.readInt();
            int qtd = buckets.readInt();

            if (k == chave) {

                List<Long> nova = new ArrayList<>();

                for (int j = 0; j < qtd; j++) {
                    long v = buckets.readLong();
                    if (v != valor) nova.add(v);
                }

                // reescreve qtdLista
                buckets.seek(posRegistro + 4);
                buckets.writeInt(nova.size());

                // reescreve valores mantidos
                buckets.seek(posRegistro + 8);
                for (long v : nova) {
                    buckets.writeLong(v);
                }

                // preenche slots restantes com -1 para não deixar lixo
                for (int j = nova.size(); j < TAM_BUCKET; j++) {
                    buckets.writeLong(-1L);
                }

                return;
            }
        }
    }


    // ATALHOS PARA USO COMO ÍNDICE PRIMÁRIO (PK)
    public void inserir(int chave, long valor) throws IOException {
        inserirLista(chave, valor);
    }

    public long buscar(int chave) throws IOException {
        List<Long> l = buscarLista(chave);
        return l.isEmpty() ? -1L : l.get(0);
    }

    public void fechar() throws IOException {
        diretorio.close();
        buckets.close();
    }
}