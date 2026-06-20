# BiblioTech – Sistema de Gerenciamento de Biblioteca

Sistema desenvolvido para a disciplina de **AEDS III** com foco na construção de um motor de banco de dados próprio, utilizando persistência em arquivos binários, Hash Extensível, Árvore B+, relacionamentos 1:N e N:N, ordenação externa e compressão de dados utilizando Huffman e LZW.

---

## Integrantes do Grupo

| Nome |
|--- |
| Matheus Mendes Ramos
| David Cristhian Vieira Fonseca 
| Letícia Beatriz da Silva Lopes
| Igor Patrick Freitas da Silva

---

## Tecnologias

- Java 17+
- Spring Boot 3 (Spring Web / Tomcat embutido)
- HTML + CSS + JavaScript (front-end)
- Arquivos binários `.dat` e `.hash` (persistência própria, sem banco de dados 
- Árvore B+
- Compressão Huffman
- Compressão LZW
externo)

---

## Pré-requisitos

- **JDK 17** ou superior instalado e no `PATH`
- **Maven 3.8+** (ou usar o wrapper `mvnw` incluído no projeto)
- Nenhuma instalação de banco de dados necessária

Verificar versões:

```bash
java -version
mvn -version
```

---

## Estrutura do Projeto

```
bibliotech/
├── src/
│   └── main/
│       ├── java/com/bibliotech/
│       │   ├── compression/
│       │   │   ├── LZW.java
│       │   │   ├── Huffman.java
│       │   │   ├── HuffmanNode.java
│       │   │   └── BackupManager.java
│       │   ├── BibliotechApplication.java   ← ponto de entrada Spring Boot
│       │   ├── controller/                  ← endpoints REST
│       │   ├── dao/                         ← persistência em arquivos binários
│       │   │   ├── HashExtensivel.java
│       │   │   ├── AutorDAO.java
│       │   │   ├── LivroDAO.java
│       │   │   ├── UsuarioDAO.java
│       │   │   └── EmprestimoDAO.java
│       │   └── model/                       ← entidades
│       └── resources/
│           ├── application.properties
│           └── static/                      ← front-end (HTML/CSS/JS)
├── data/                                    ← criado automaticamente na primeira 
backup/
├── backup_completo.dat
├── backup_completo.lzw
└── backup_completo.huff
execução
│   ├── autores.dat
│   ├── livros.dat
│   ├── usuarios.dat
│   ├── emprestimos.dat
│   ├── diretorios/   ← arquivos de diretório da Hash Extensível
│   └── buckets/      ← arquivos de bucket da Hash Extensível
├── temp1.dat
├── temp2.dat
├── temp3.dat
├── temp4.dat
├── pom.xml
└── README.md
```

> A pasta `data/` é criada automaticamente ao iniciar a aplicação. Não é necessário criá-la manualmente.

---

## Como Compilar e Executar

Pela IDE (IntelliJ / Eclipse / VS Code)

1. Abrir o projeto como projeto Maven
2. Localizar a classe `BibliotechApplication.java`
3. Clicar com o botão direito → **Run as Java Application** (ou usar o botão ▶ da IDE)

---

## Acessando o Sistema

Após iniciar, a aplicação estará disponível em:

```
http://localhost:8080
```

Realizar Cadastro caso seja o primeiro acesso

---

O front-end é servido automaticamente pela pasta `src/main/resources/static/`.
Na **primeira execução**, um usuário administrador padrão é criado automaticamente:
 
| Campo | Valor |
|---|---|
| E-mail | `admin@bibliotech.com` |
| Senha | `admin@#$` |
 
---

## Endpoints da API REST

Todos os endpoints têm prefixo `/api`.

### Usuários — `/api/usuarios`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/usuarios` | Lista todos os usuários |
| `GET` | `/api/usuarios/{id}` | Busca usuário por ID |
| `POST` | `/api/usuarios` | Cria novo usuário (admin) |
| `POST` | `/api/usuarios/cadastrar` | Auto-cadastro (tipo Standard) |
| `POST` | `/api/usuarios/login` | Autenticação |
| `PUT` | `/api/usuarios/{id}` | Atualiza usuário |
| `DELETE` | `/api/usuarios/{id}` | Remove usuário |

### Autores — `/api/autores`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/autores` | Lista todos os autores |
| `GET` | `/api/autores/{id}` | Busca autor por ID |
| `POST` | `/api/autores` | Cria novo autor |
| `PUT` | `/api/autores/{id}` | Atualiza autor |
| `DELETE` | `/api/autores/{id}` | Remove autor |

### Livros — `/api/livros`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/livros` | Lista todos os livros |
| `GET` | `/api/livros/{id}` | Busca livro por ID |
| `GET` | `/api/livros/autor/{idAutor}` | Lista livros de um autor (índice 1:N) |
| `POST` | `/api/livros` | Cria novo livro |
| `PUT` | `/api/livros/{id}` | Atualiza livro |
| `DELETE` | `/api/livros/{id}` | Remove livro |

### Empréstimos — `/api/emprestimos`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/emprestimos` | Lista todos os empréstimos |
| `GET` | `/api/emprestimos/{id}` | Busca empréstimo por ID |
| `GET` | `/api/emprestimos/usuario/{id}` | Empréstimos de um usuário (índice 1:N) |
| `GET` | `/api/emprestimos/livro/{id}` | Empréstimos de um livro (índice 1:N) |
| `POST` | `/api/emprestimos` | Registra novo empréstimo |
| `PUT` | `/api/emprestimos/{id}` | Atualiza empréstimo (ex: devolução) |
| `DELETE` | `/api/emprestimos/{id}` | Remove empréstimo |

---

## Arquitetura de Persistência

### Arquivos de dados (`.dat`)

Cada entidade possui um arquivo `.dat` com a seguinte estrutura:

```
[ cabeçalho: 8 bytes ]
  └── ultimoID (int, 4 bytes)
  └── quantidade (int, 4 bytes)

[ registros sequenciais ]
  └── ativo (boolean, 1 byte)   ← lápide: false = excluído logicamente
  └── tamanho (int, 4 bytes)    ← tamanho do payload em bytes
  └── dados (byte[tamanho])     ← objeto serializado via DataOutputStream
```

### Índices — Hash Extensível

Cada DAO mantém índices em disco na pasta `data/diretorios/` e `data/buckets/`:

| Arquivo | Tipo | Mapeamento |
|---|---|---|
| `autores_*.hash` | PK | `idAutor → posição no .dat` |
| `livros_*.hash` | PK | `idLivro → posição no .dat` |
| `autorlivros_*.hash` | 1:N | `idAutor → [posições de livros]` |
| `usuarios_*.hash` | PK | `idUsuario → posição no .dat` |
| `emprestimos_*.hash` | PK | `idEmprestimo → posição no .dat` |
| `emp_usuario_*.hash` | 1:N | `idUsuario → [posições de empréstimos]` |
| `emp_livro_*.hash` | 1:N | `idLivro → [posições de empréstimos]` |

### Acesso ao relacionamento 1:N

```
buscarLivrosPorAutor(idAutor):
  1. hashAutorLivros.buscarLista(idAutor) → [pos1, pos2, pos3]
  2. Para cada posição → arquivo.seek(pos) → lê registro diretamente
  Complexidade: O(k), k = número de livros do autor. Sem varredura linear.
```

### Reconstrução automática de índices

Se os arquivos `.hash` estiverem ausentes ou incompatíveis (ex: após alterar `TAM_BUCKET`), os índices são reconstruídos automaticamente na inicialização percorrendo o `.dat` do início ao fim.

---
### Pesquisa por Padrão (KMP) — `/livros`
 
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/livros/busca-padrao?padrao=...&algoritmo=KMP&campo=titulo` | Busca livros cujo título, autor ou gênero contenham o padrão informado |
 
---
 
## Casamento de Padrões — KMP (Knuth-Morris-Pratt)
 
### Campo escolhido
 
A busca por padrão foi aplicada sobre os campos textuais do livro: **título**, **autor** e **gênero**, com a opção de busca combinada em todos eles (`campo=todos`). Esses são os únicos campos de texto livre do modelo `Livro` — preço e data de publicação são valores numéricos/data, sem sentido para uma busca por substring.
 
### Onde está o código
 
```
src/main/java/com/bibliotech/util/KMP.java
src/main/java/com/bibliotech/controller/PesquisaController.java
```
 
### Como funciona
 
1. **Pré-processamento (array LPS):** antes de procurar no texto, o algoritmo monta um array auxiliar (`lps[]`) a partir do próprio padrão, indicando o maior prefixo que também é sufixo em cada posição.
2. **Busca:** o texto é percorrido uma única vez. Ao encontrar uma divergência, o índice do padrão recua usando o `lps[]` (sem retroceder no texto), evitando comparações repetidas.
3. **Complexidade:** O(n + m), onde `n` é o tamanho do texto e `m` o tamanho do padrão.
4. A busca é *case-insensitive* (texto e padrão são convertidos para minúsculas antes da comparação).
### Como compilar
 
O `KMP.java` é compilado automaticamente junto com o restante do projeto pelo Maven — não exige nenhum passo extra:
 
```bash
# na raiz do projeto
mvn clean compile
```
 
Ou, se preferir compilar manualmente apenas essa classe (fora do Maven), a partir da raiz do projeto:
 
```bash
javac -d target/classes src/main/java/com/bibliotech/util/KMP.java
```
 
### Como executar e testar
 
1. Suba a aplicação normalmente:
```bash
   mvn spring-boot:run
```
   ou execute `BibliotechApplication.java` pela IDE.
 
2. Acesse a tela **Pesquisa Avançada** em `http://localhost:8080/pesquisa`, preencha o campo "Padrão de busca", escolha o algoritmo (`KMP`) e o campo (`Título`, `Autor`, `Gênero` ou `Todos`), e clique em **Buscar por Padrão**.
3. Ou teste diretamente via terminal, sem usar a interface:
```bash
   curl "http://localhost:8080/livros/busca-padrao?padrao=anel&algoritmo=KMP&campo=titulo"
```
 
   Resposta esperada (exemplo):
```json
   {
     "padrao": "anel",
     "algoritmo": "KMP",
     "campo": "titulo",
     "totalResultados": 1,
     "resultados": [
       {
         "titulo": "O Senhor dos Anéis",
         "ocorrenciasTitulo": [13],
         "totalOcorrencias": 1
       }
     ]
   }
```
 
---

## Segurança

### Armazenamento de Senhas

As senhas dos usuários são protegidas utilizando uma combinação de **criptografia simétrica baseada em XOR** e **codificação Base64**.

O processo funciona da seguinte forma:

1. A senha informada pelo usuário é convertida para bytes UTF-8;
2. Cada byte é combinado, por meio da operação **XOR**, com uma chave fixa (`"pucminas"`);
3. O resultado é codificado em **Base64**, garantindo que os dados possam ser armazenados com segurança em arquivos binários utilizando `writeUTF()`.

Fluxo de armazenamento:

```text
Senha em texto puro → XOR → Base64 → arquivo .dat
````

Durante a autenticação, a senha fornecida pelo usuário é novamente processada com o mesmo algoritmo e comparada ao valor armazenado.

```text
Senha digitada → XOR → Base64 → comparação
```

Como a operação XOR é reversível, o sistema pode recuperar a senha original quando necessário:

```text
(A XOR B) XOR B = A
```

### Implementação

A lógica de proteção das senhas está centralizada na classe:

```text
src/main/java/com/bibliotech/dao/UsuarioDAO.java
```

Métodos responsáveis:

* `criptografar(String senha)`
* `descriptografar(String senhaCriptografada)`

### Limitações

> **Importante:** o uso de XOR com chave fixa não é considerado seguro para aplicações em produção.

Essa abordagem foi adotada exclusivamente para fins acadêmicos, com o objetivo de demonstrar conceitos de transformação de dados e persistência em arquivos binários.

Em sistemas reais, recomenda-se utilizar algoritmos específicos para armazenamento de senhas, como:

* BCrypt
* Argon2
* PBKDF2

Esses algoritmos utilizam técnicas como *salt*, múltiplas iterações e custo computacional ajustável, impedindo a recuperação da senha original.

---

## Compressão

O sistema gera automaticamente um backup único contendo todos os arquivos de dados utilizados pela aplicação.

Arquivos incluídos:

- autores.dat
- livros.dat
- usuarios.dat
- emprestimos.dat

Os arquivos são reunidos em:

backup_completo.dat

Sobre este arquivo são aplicados dois algoritmos de compressão:

- LZW
- Huffman

Arquivos gerados:

backup_completo.lzw
backup_completo.huff

### Resultados obtidos:

LZW
- Arquivo original: 6012 bytes
- Arquivo compactado: 5372 bytes
- Taxa de compressão: 10,65%

Huffman
- Arquivo original: 6012 bytes
- Arquivo compactado: 3959 bytes
- Taxa de compressão: 34,15%

Nos testes realizados, o algoritmo Huffman apresentou melhor desempenho para os dados do sistema.

## Licença

Projeto acadêmico desenvolvido para fins educacionais — AEDS III, PUC Minas.