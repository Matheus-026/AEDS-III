# BiblioTech – Sistema de Gerenciamento de Biblioteca

Sistema desenvolvido para a disciplina de **AEDS III** com foco na construção de um motor de banco de dados próprio, utilizando persistência em arquivos binários e técnicas avançadas de indexação.

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
- Arquivos binários `.dat` e `.hash` (persistência própria, sem banco de dados externo)

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
├── data/                                    ← criado automaticamente na primeira execução
│   ├── autores.dat
│   ├── livros.dat
│   ├── usuarios.dat
│   ├── emprestimos.dat
│   ├── diretorios/   ← arquivos de diretório da Hash Extensível
│   └── buckets/      ← arquivos de bucket da Hash Extensível
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

## Segurança

- Senhas armazenadas com **criptografia XOR** 
- O DAO sempre grava senha criptografada e retorna senha descriptografada
- Senhas nunca são expostas nos endpoints de listagem (`setSenha(null)` antes de serializar)

---

## Licença

Projeto acadêmico desenvolvido para fins educacionais — AEDS III, PUC Minas.
