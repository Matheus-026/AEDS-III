// ─── Configuração da API ──────────────────────────────────────────────────────
const API_URL = "http://localhost:8080/api/livros";
const API_AUTORES = "http://localhost:8080/api/autores";

const modal = document.getElementById("modalLivro");
const form = document.getElementById("formLivro");
const tbody = document.querySelector("tbody");

// gêneros (multivalorado)
const inputGenero = document.getElementById("genero-input");
const containerGeneros = document.getElementById("generos-container");
let generosSelecionados = [];
let autoresMap = {};

// AUTOCOMPLETE
let autores = [];
let autorSelecionadoId = null;

const inputAutor = document.getElementById("autorInput");
const listaAutores = document.getElementById("listaAutores");

// controle de edição
let modoEdicao = false;
let idEditando = null;


// GÊNEROS

if (inputGenero) {
    inputGenero.addEventListener("keydown", function (e) {
        if (e.key === "Enter") {
            e.preventDefault();

            const valor = inputGenero.value.trim();

            if (valor && !generosSelecionados.includes(valor)) {
                generosSelecionados.push(valor);
                renderGeneros();
            }

            inputGenero.value = "";
        }
    });
}

function renderGeneros() {
    containerGeneros.innerHTML = "";

    generosSelecionados.forEach((genero, index) => {
        const tag = document.createElement("div");
        tag.classList.add("genero-tag");

        tag.innerHTML = `
            ${genero}
            <span onclick="removerGenero(${index})">x</span>
        `;

        containerGeneros.appendChild(tag);
    });

    if (inputGenero) {
        containerGeneros.appendChild(inputGenero);
    }
}

function removerGenero(index) {
    generosSelecionados.splice(index, 1);
    renderGeneros();
}

// MODAL

function abrirModal() {
    modal.style.display = "flex";
    carregarAutores();
}

function fecharModal() {
    modal.style.display = "none";

    modoEdicao = false;
    idEditando = null;

    document.getElementById("modal-titulo").textContent = "Cadastrar Livro";
    document.getElementById("btn-submit").textContent = "Adicionar";

    form.reset();

    generosSelecionados = [];
    renderGeneros();

    if (inputAutor) inputAutor.value = "";
    autorSelecionadoId = null;
}

window.onclick = function(event) {
    if (event.target === modal) {
        fecharModal();
    }
};


// CARREGAR LIVROS

async function carregarLivros() {
    await carregarAutoresMap();

    try {
        const response = await fetch(API_URL);
        const livros = await response.json();

        tbody.innerHTML = "";

        livros.forEach(livro => {
            adicionarNaTabela(livro);
        });

    } catch (error) {
        console.error("Erro ao carregar livros:", error);
    }
}


// TABELA

function adicionarNaTabela(livro) {

    const tr = document.createElement("tr");

    tr.innerHTML = `
        <td>${livro.titulo}</td>
        <td>${autoresMap[livro.idAutor] || "-"}</td>
        <td>${Array.isArray(livro.generos) ? livro.generos.join(", ") : livro.generos}</td>
        <td>R$ ${Number(livro.preco).toFixed(2)}</td>
        <td>${formatarData(livro.dataPublicacao)}</td>
        <td class="acoes">
            <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>

            <div class="menu-acoes">
                <div class="item" onclick="abrirEdicao(${livro.id})">Editar</div>
                <div class="item" onclick="excluirLivro(${livro.id})">Excluir</div>
            </div>
        </td>
    `;

    tbody.appendChild(tr);
}


// EDITAR

async function abrirEdicao(id) {
    try {
        const res = await fetch(`${API_URL}/${id}`);
        const livro = await res.json();

        modoEdicao = true;
        idEditando = id;

        document.getElementById("modal-titulo").textContent = "Editar Livro";
        document.getElementById("btn-submit").textContent = "Salvar";

        document.getElementById("titulo").value = livro.titulo;
        document.getElementById("data").value = livro.dataPublicacao;
        document.getElementById("preco").value = livro.preco;

        generosSelecionados = livro.generos || [];
        renderGeneros();

        if (inputAutor) {
            inputAutor.value = autoresMap[livro.idAutor] || "";
            autorSelecionadoId = livro.idAutor;
        }

        abrirModal();

    } catch (error) {
        console.error(error);
        alert("Erro ao carregar livro para edição");
    }
}


// EXCLUIR

async function excluirLivro(id) {
    fecharMenus();

    if (!confirm("Deseja realmente excluir este livro?")) return;

    try {
        const res = await fetch(`${API_URL}/${id}`, {
            method: "DELETE"
        });

        if (!res.ok) throw new Error();

        alert("Livro excluído!");
        carregarLivros();

    } catch (error) {
        console.error(error);
        alert("Erro ao excluir livro");
    }
}


// SALVAR EDIÇÃO

async function salvarEdicao(livro) {
    try {
		const url = modoEdicao
		    ? `${API_URL}/${idEditando}`
		    : API_URL;

		const method = modoEdicao ? "PUT" : "POST";

		const response = await fetch(url, {
		    method: method,
		    headers: {
		        "Content-Type": "application/json"
		    },
		    body: JSON.stringify(livro)
		});

        if (!res.ok) {
            const msg = await res.text();
            alert("❌ " + msg);
            return;
        }

        alert("Livro atualizado!");
        carregarLivros();
        fecharModal();

    } catch (error) {
        console.error(error);
        alert("Erro ao atualizar livro");
    }
}


// CREATE + UPDATE

form.addEventListener("submit", async function(e) {
    e.preventDefault();

    const titulo = document.getElementById("titulo").value;
    const data = document.getElementById("data").value;
    const preco = document.getElementById("preco").value;

    let idAutorFinal = autorSelecionadoId;

    if (!idAutorFinal) {
        const nomeDigitado = inputAutor.value.trim();

        if (!nomeDigitado) {
            alert("Digite ou selecione um autor!");
            return;
        }

        idAutorFinal = await criarAutor(nomeDigitado);
    }

    const livro = {
        titulo,
        resumo: "",
        preco: parseFloat(preco),
        dataPublicacao: data,
        generos: generosSelecionados,
        idAutor: idAutorFinal
    };

    try {

        const url = modoEdicao
            ? `${API_URL}/${idEditando}`
            : API_URL;

        const method = modoEdicao ? "PUT" : "POST";

        const response = await fetch(url, {
            method: method,
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(livro)
        });

        if (!response.ok) {
            const msg = await response.text();
            alert("❌ " + msg);
            return;
        }

        if (modoEdicao) {
            alert("Livro atualizado!");
        } else {
            const result = await response.json();
            adicionarNaTabela({ ...livro, id: result.id });
            alert("Livro salvo!");
        }

        fecharModal();
        form.reset();
        generosSelecionados = [];
        renderGeneros();

    } catch (error) {
        console.error(error);
        alert("Erro ao salvar livro");
    }
});


// AUTOR
async function carregarAutores() {
    const res = await fetch(API_AUTORES);
    autores = await res.json();
}

async function carregarAutoresMap() {
    const res = await fetch(API_AUTORES);
    const lista = await res.json();

    autoresMap = {};
    lista.forEach(a => {
        autoresMap[a.id] = a.nome;
    });
}

async function criarAutor(nome) {
    const res = await fetch(API_AUTORES, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ nome })
    });

    if (!res.ok) throw new Error();

    const data = await res.json();
    return data.id;
}


// AUTOCOMPLETE

if (inputAutor) {
    inputAutor.addEventListener("input", function () {

        const valor = this.value.toLowerCase();
        listaAutores.innerHTML = "";

        if (!valor) {
            listaAutores.style.display = "none";
            autorSelecionadoId = null;
            return;
        }

        const filtrados = autores.filter(a =>
            a.nome.toLowerCase().includes(valor)
        );

        filtrados.forEach(a => {
            const div = document.createElement("div");
            div.classList.add("item-sugestao");
            div.textContent = a.nome;

            div.onclick = () => {
                inputAutor.value = a.nome;
                autorSelecionadoId = a.id;
                listaAutores.style.display = "none";
            };

            listaAutores.appendChild(div);
        });

        listaAutores.style.display = filtrados.length ? "block" : "none";
    });
}


// HELPERS

function toggleMenu(botao) {
    const menu = botao.nextElementSibling;

    document.querySelectorAll('.menu-acoes').forEach(m => {
        if (m !== menu) m.style.display = 'none';
    });

    menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
}

function fecharMenus() {
    document.querySelectorAll('.menu-acoes').forEach(m => m.style.display = 'none');
}

function formatarData(data) {
    return new Date(data).toLocaleDateString("pt-BR");
}


// BUSCA HÍBRIDA

let livrosCache = [];

// normalizar texto (acentos)
function normalizar(texto) {
    return texto
        .toLowerCase()
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "");
}

// sobrescreve carregarLivros pra guardar cache 
const carregarLivrosOriginal = carregarLivros;

carregarLivros = async function() {
    await carregarAutoresMap();

    try {
        const response = await fetch(API_URL);
        const livros = await response.json();

        livrosCache = livros; 

        tbody.innerHTML = "";

        livros.forEach(livro => {
            adicionarNaTabela(livro);
        });

    } catch (error) {
        console.error("Erro ao carregar livros:", error);
    }
};

const precoInput = document.getElementById("preco");

precoInput.addEventListener("focus", () => {
    if (precoInput.value === "0.00") {
        precoInput.value = "";
    }
});

precoInput.addEventListener("blur", () => {
    let valor = Number(precoInput.value);

    if (isNaN(valor) || precoInput.value.trim() === "") {
        precoInput.value = "0.00";
    } else {
        precoInput.value = valor.toFixed(2);
    }
});


// render tabela
function renderTabela(livros) {
    tbody.innerHTML = "";
    livros.forEach(l => adicionarNaTabela(l));
}

// evento de busca
document.getElementById("buscar").addEventListener("input", async (e) => {

    const valor = e.target.value.trim();
    const tipo = document.getElementById("tipoBusca").value;

    // vazio → mostra tudo
    if (valor === "") {
        renderTabela(livrosCache);
        return;
    }

    // QUALQUER CASO → se for número usa HASH (ID)
    if (!isNaN(valor)) {
        try {
            const res = await fetch(`${API_URL}/${valor}`);

            if (!res.ok) {
                tbody.innerHTML = "<tr><td colspan='5'>Não encontrado</td></tr>";
                return;
            }

            const livro = await res.json();
            renderTabela([livro]);

        } catch {
            tbody.innerHTML = "<tr><td colspan='5'>Livro não encontrado</td></tr>";
        }

        return;
    }

    const termo = normalizar(valor);

    
    if (tipo === "titulo") {
        const filtrados = livrosCache.filter(l =>
            normalizar(l.titulo).includes(termo)
        );

        renderTabela(filtrados);
        return;
    }

 
    if (tipo === "autor") {
        const filtrados = livrosCache.filter(l => {
            const nomeAutor = autoresMap[l.idAutor] || "";
            return normalizar(nomeAutor).includes(termo);
        });
		

        renderTabela(filtrados);
        return;
    }
});

// ==========================================
// ORDENAÇÃO EXTERNA (GATILHO DO SELECT)
// ==========================================
document.getElementById('ordenar').addEventListener('change', async function() {
    
    // Verifica se a opção escolhida foi o Preço
    if (this.value === 'preco') {
        
        // Bloqueia o select para o utilizador não clicar várias vezes enquanto ordena
        this.disabled = true; 
        
        // Se já tiver uma função de Toast, use-a. Se não, um alert avisa o utilizador.
        alert("Iniciando a Ordenação Externa em disco... Aguarde.");

        try {
            // Dispara a requisição POST para a rota que criamos no Controller
            const response = await fetch('http://localhost:8080/api/livros/ordenar', {
                method: 'POST'
            });

            if (response.ok) {
                alert("Sucesso! O ficheiro livros.dat foi ordenado pelo Preço.");
                
                // Recarrega a página para o LivroDAO ler o ficheiro na nova ordem
                window.location.reload(); 
            } else {
                alert("Erro no servidor ao tentar ordenar os livros.");
                this.disabled = false;
                this.value = ""; // Reseta o select
            }
        } catch (error) {
            console.error("Erro na conexão:", error);
            alert("Servidor offline ou erro de rede.");
            this.disabled = false;
            this.value = "";
        }
    }
});

window.onload = carregarLivros;