# BiblioTech – Sistema de Gerenciamento de Biblioteca

Sistema desenvolvido para a disciplina de **AEDS III**, com foco na construção de um motor de banco de dados próprio, utilizando persistência em arquivos binários e técnicas avançadas de indexação e compressão.

---

##  Integrantes do Grupo

- Matheus Mendes Ramos  
- David Cristhian Vieira Fonseca  
- Letícia Beatriz da Silva Lopes  
- Igor Patrick Freitas da Silva  

---

##  1. Descrição do Problema

Bibliotecas comunitárias e pequenas livrarias necessitam de um sistema robusto para gerenciar:

-  Acervo de livros  
-  Autores  
-  Usuários  
-  Empréstimos  

Sem depender de servidores de banco de dados tradicionais.

O sistema realiza a persistência diretamente em **memória secundária**, utilizando arquivos binários e estruturas de indexação próprias.

---

##  2. Objetivo do Projeto

- Desenvolver um **motor de banco de dados próprio** com operações CRUD.
- Implementar:
  -  Árvore B+
  -  Hash Extensível
  -  Criptografia XOR
  -  Compactação Huffman/LZW
- Garantir integridade e eficiência na manipulação de dados em arquivos `.dat` ou `.bin`.

---

##  3. Requisitos Funcionais

###  RF01 – Cadastro de Livro
- Título (String)
- Preço (Real)
- Data de publicação (Data)
- Tags/Gêneros (Lista multivalorada)

###  RF02 – Gerenciamento de Autores
- Cadastro e associação 1:N com livros

###  RF03 – Gerenciamento de Empréstimos
- Relacionamento N:N entre Usuários e Livros
- Controle de datas e devoluções

###  RF04 – Pesquisa Avançada
- Busca textual utilizando:
  - KMP
  - Boyer-Moore

###  RF05 – Autenticação
- Login de administrador
- Senha protegida com criptografia XOR

###  RF06 – Manutenção de Espaço
- Compactação e descompactação para backup

###  RF07 – Listagem Ordenada
- Exibição de livros por:
  - Título
  - Data
- Utilizando Árvore B+

---

##  4. Requisitos Não Funcionais

- Interface gráfica obrigatória (Web ou Desktop)
- Persistência direta em arquivos binários
- Exclusão lógica com uso de lápide
- Uso de cabeçalho nos arquivos para controle de metadados

---

##  5. Atores do Sistema

###  Bibliotecário (Admin)
- Gerencia livros, autores e usuários
- Realiza empréstimos

###  Leitor
- Consulta disponibilidade
- Realiza buscas textuais

---

##  6. Modelagem (DER – Lógico)

Relacionamentos principais:

- Autor (1) — (N) Livro  
- Livro (1) — (N) Empréstimo  
- Usuário (1) — (N) Empréstimo  
- Relação N:N via tabela intermediária `Item_Emprestimo`

### Atributos obrigatórios:
- Data (Publicação/Devolução)
- Real (Preço/Multa)
- String multivalorada (Gêneros/Telefones)

---

## 7. Arquitetura

O sistema segue o padrão:

###  MVC + DAO

- **View** → Interface gráfica (HTML/CSS/JS ou Java Swing/JavaFX)  
- **Controller** → Regras de negócio  
- **Model** → `Livro`, `Autor`, `Usuario`, `Emprestimo`  
- **DAO** → Manipulação de arquivos binários, controle de offsets e indexação  

Responsabilidades da camada DAO:

- Abertura e manipulação de arquivos
- Controle de cabeçalho
- Exclusão lógica (lápide)
- Gerenciamento da Árvore B+ e Hash Extensível

---

## Persistência de Dados

- Arquivos `.dat` ou `.bin`
- Controle de metadados via cabeçalho
- Uso de ponteiros (offset)
- Exclusão lógica
- Estruturas auxiliares de indexação

---

## Tecnologias Utilizadas

- Java  
- Estruturas de Dados Avançadas  
- Manipulação de Arquivos Binários  
- Algoritmos de Busca Textual  
- Compressão de Dados  

---

## Como Executar

1. Clone o repositório:

```bash
git clone <url-do-repositorio>
```
---
## 📌Considerações Finais

O BiblioTech foi desenvolvido com foco em:

- Implementação prática de estruturas de dados

- Manipulação de arquivos em baixo nível

- Construção de um mini SGBD

- Aplicação de conceitos avançados vistos na disciplina

---
## 📄Licença

Projeto acadêmico desenvolvido para fins educacionais.
