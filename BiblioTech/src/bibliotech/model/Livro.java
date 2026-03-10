package bibliotech.model;

import java.io.*;
import java.time.LocalDate;

public class Livro implements Registro{
    private int id;
    private String titulo;
    private String resumo;
    private float preco;
    private LocalDate dataPublicacao;
    private String[] generos;
    

    public Livro(){
        this(-1, "", "", 0F, LocalDate.now(), new String[0]);
    }

    public Livro(String titulo, String resumo, float preco, LocalDate dataPublicacao, String[] generos) {
        this(-1, titulo, resumo, preco, dataPublicacao, generos);
    }

    public Livro(int id, String titulo, String resumo, float preco, LocalDate dataPublicacao, String[] generos){
        this.id = id;
        this.titulo = titulo;
        this.resumo = resumo;
        this.preco = preco;
        this.dataPublicacao = dataPublicacao;
        this.generos = generos;
    }

    public void setId(int id){this.id = id;}
    public int getId(){return id;}

    public void setTitulo(String titulo){this.titulo = titulo;}
    public String getTitulo(){return titulo;}

    public void setResumo(String resumo){this.resumo = resumo;}
    public String getResumo(){return resumo;}

    public void setPreco(float preco){this.preco = preco;}
    public float getPreco(){return preco;}

    public void setDataPublicacao(LocalDate dataPublicacao){this.dataPublicacao = dataPublicacao;}
    public LocalDate getDataPublicacao(){return dataPublicacao;}

    public void setGeneros(String[] generos){this.generos = generos;}
    public String[] getGeneros(){return generos;}


    public byte[] toByteArray() throws IOException{
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(ba);

        dos.writeInt(this.id);
        dos.writeUTF(this.titulo);
        dos.writeUTF(this.resumo);
        dos.writeFloat(this.preco);
        dos.writeLong(this.dataPublicacao.toEpochDay());
        dos.writeInt(this.generos.length);
        for(String genero : this.generos){
            dos.writeUTF(genero);
        }
        return ba.toByteArray();
    }

    public void fromByteArray(byte[] ba) throws IOException{
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        this.id = dis.readInt();
        this.titulo = dis.readUTF();
        this.resumo = dis.readUTF();
        this.preco = dis.readFloat();
        this.dataPublicacao = LocalDate.ofEpochDay(dis.readLong());

        int qtdGeneros = dis.readInt();
        this.generos = new String[qtdGeneros];
        for(int i = 0; i < qtdGeneros; i++){
            this.generos[i] = dis.readUTF();
        }
    }

    public String toString(){
        return "ID: " + id + " | Título: " + titulo + " | Preço: R$" + preco + " | Publicado em: " + dataPublicacao + " | Gêneros: " + String.join(", ", generos);
    }
}