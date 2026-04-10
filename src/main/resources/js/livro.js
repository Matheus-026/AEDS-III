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

// 🔥 AUTOCOMPLETE
let autores = [];
let autorSelecionadoId = null;

const inputAutor = document.getElementById("autorInput");
const listaAutores = document.getElementById("listaAutores");

// controle de edição
let modoEdicao = false;
let idEditando = null;

// =========================
// GÊNEROS (MULTIVALORADO)
// =========================
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

// =========================
// MODAL
// =========================
function abrirModal() {
    modal.style.display = "flex";
    carregarAutores(); // 🔥 autocomplete
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

    // limpar autor
    if (inputAutor) inputAutor.value = "";
    autorSelecionadoId = null;
}

window.onclick = function(event) {
    if (event.target === modal) {
        fecharModal();
    }
};

// =========================
// CARREGAR LIVROS
// =========================
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

// =========================
// ADICIONAR NA TABELA
// =========================
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
                <div class="item" onclick="abrirEdicao(${livro.id})">
                    Editar
                </div>

                <div class="item" onclick="excluirLivro(${livro.id})">
                    Excluir
                </div>
            </div>
        </td>
    `;

    tbody.appendChild(tr);
}

// =========================
// ABRIR EDIÇÃO
// =========================
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

        // 🔥 seta nome do autor no input
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

// =========================
// EXCLUIR LIVRO
// =========================
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

// =========================
// SALVAR EDIÇÃO
// =========================
async function salvarEdicao(livro) {
    try {
        const res = await fetch(`${API_URL}/${idEditando}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(livro)
        });

        if (!res.ok) throw new Error();

        alert("Livro atualizado!");
        carregarLivros();
        fecharModal();

    } catch (error) {
        console.error(error);
        alert("Erro ao atualizar livro");
    }
}

// =========================
// SUBMIT (CREATE + UPDATE)
// =========================
form.addEventListener("submit", async function(e) {
    e.preventDefault();

    const titulo = document.getElementById("titulo").value;
    const data = document.getElementById("data").value;
    const preco = document.getElementById("preco").value;

    let idAutorFinal = autorSelecionadoId;

    // 🔥 se não selecionou, cria autor
    if (!idAutorFinal) {
        const nomeDigitado = inputAutor.value.trim();

        if (!nomeDigitado) {
            alert("Digite ou selecione um autor!");
            return;
        }

        try {
            idAutorFinal = await criarAutor(nomeDigitado);
			
        } catch (error) {
            alert("Erro ao criar autor");
            return;
        }
    }

    const livro = {
        titulo: titulo,
        resumo: "",
        preco: parseFloat(preco),
        dataPublicacao: data,
        generos: generosSelecionados,
        idAutor: idAutorFinal
    };

    if (modoEdicao) {
        await salvarEdicao(livro);
        modoEdicao = false;
        idEditando = null;
        return;
    }

    try {
        const response = await fetch(API_URL, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(livro)
        });

        if (!response.ok) throw new Error();

        const result = await response.json();

        adicionarNaTabela({
            ...livro,
            id: result.id
        });

        form.reset();
        generosSelecionados = [];
        renderGeneros();
        fecharModal();

        alert("Livro salvo!");

    } catch (error) {
        console.error(error);
        alert("Erro ao salvar livro");
    }
});

// =========================
// AUTOCOMPLETE
// =========================
async function carregarAutores() {
    const res = await fetch(API_AUTORES);
    autores = await res.json();
}

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

document.addEventListener("click", (e) => {
    if (!e.target.closest(".autocomplete-container")) {
        if (listaAutores) listaAutores.style.display = "none";
    }
});

// =========================
// MENU (⋮)
// =========================
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

document.addEventListener('click', (e) => {
    if (!e.target.closest('.acoes')) fecharMenus();
});

// =========================
// FORMATAR DATA
// =========================
function formatarData(data) {
    return new Date(data).toLocaleDateString("pt-BR");
}

async function carregarAutoresMap() {
    const res = await fetch(API_AUTORES);
    const autores = await res.json();

    autoresMap = {};
    autores.forEach(a => {
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

// carregar ao abrir página
window.onload = carregarLivros;