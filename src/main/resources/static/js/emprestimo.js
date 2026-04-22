// ─── Configuração da API ──────────────────────────────────────────────────────
const API_URL      = "http://localhost:8080/api/emprestimos";
const API_LIVROS   = "http://localhost:8080/api/livros";
const API_USUARIOS = "http://localhost:8080/api/usuarios";

// caches
let livrosCache      = [];
let emprestimosCache = [];
let usuariosCache = [];

// ─── Navbar hide on scroll ────────────────────────────────────────────────────
const navbar = document.querySelector('.navbar');
let ultimoScroll = 0;

window.addEventListener('scroll', () => {
    const scrollAtual = window.scrollY;
    if (scrollAtual > ultimoScroll && scrollAtual > 200) {
        navbar.classList.add('escondida');
    } else {
        navbar.classList.remove('escondida');
    }
    ultimoScroll = scrollAtual;
});

// ─── Estado de edição ─────────────────────────────────────────────────────────
let modoEdicao = false;
let idEditando  = null;

// ─── Elementos de autocomplete ────────────────────────────────────────────────
const inputUsuario    = document.getElementById("input-usuario-nome");
const sugestoesUsuario = document.getElementById("lista-usuarios");

const inputLivro    = document.getElementById("input-livro-nome");
const sugestoesLivro = document.getElementById("lista-livros");

// ─── Ao carregar a página ─────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", async () => {
    await Promise.all([carregarLivros(), carregarUsuarios()]);
    carregarEmprestimos();

    document.getElementById("search_emprestimo").addEventListener("input", aplicarFiltros);

    document.getElementById("formEmprestimo").addEventListener("submit", async (e) => {
        e.preventDefault();
        if (modoEdicao) {
            await salvarEdicao();
        } else {
            await criarEmprestimo();
        }
    });

    document.getElementById("search_emprestimo").addEventListener("input", (e) => {
        const termo = e.target.value.toLowerCase();
        const filtrados = emprestimosCache.filter(emp =>
            (emp.nomeUsuario  || "").toLowerCase().includes(termo) ||
            (emp.tituloLivro  || "").toLowerCase().includes(termo) ||
            (emp.status       || "").toLowerCase().includes(termo)
        );
        renderizarTabela(filtrados);
    });

    configurarAutocomplete(
        inputUsuario,
        sugestoesUsuario,
        () => usuariosCache,
        u => u.nome,
        u => u.id
    );

    document.getElementById("search_emprestimo").addEventListener("input", (e) => {
        const termo = e.target.value.toLowerCase();
        const filtrados = emprestimosCache.filter(emp =>
            (emp.nomeUsuario  || "").toLowerCase().includes(termo) ||
            (emp.tituloLivro  || "").toLowerCase().includes(termo) ||
            (emp.status       || "").toLowerCase().includes(termo)
        );
        renderizarTabela(filtrados);
    });

    configurarAutocomplete(
        inputLivro,
        sugestoesLivro,
        () => livrosCache,
        l => l.titulo,
        l => l.id
    );
});

// ─── Carregar dados ───────────────────────────────────────────────────────────
async function carregarLivros() {
    try {
        const res = await fetch(API_LIVROS);
        livrosCache = await res.json();
    } catch (err) {
        console.error("Erro ao carregar livros:", err);
        mostrarToast("Erro ao carregar livros.", "erro");
    }
}

async function carregarUsuarios() {
    try {
        const res = await fetch(API_USUARIOS);
        usuariosCache = await res.json();
    } catch (err) {
        console.error("Erro ao carregar usuários:", err);
        mostrarToast("Erro ao carregar usuários.", "erro");
    }
}

// ─── Autocomplete genérico ────────────────────────────────────────────────────
// Funciona para qualquer lista: basta passar o input, o div de sugestões,
// uma função que retorna o cache, e funções para extrair label e id de cada item.
function configurarAutocomplete(input, sugestoesDiv, getCache, getLabel, getId) {
    input.addEventListener("input", () => {
        const texto = input.value.toLowerCase().trim();
        sugestoesDiv.innerHTML = "";
        input.dataset.id = "";

        if (!texto) return;

        const filtrados = getCache().filter(item =>
            getLabel(item).toLowerCase().includes(texto)
        );

        filtrados.slice(0, 5).forEach(item => {
            const div = document.createElement("div");
            div.classList.add("item-sugestao");
            div.textContent = getLabel(item);

            div.addEventListener("click", (e) => {
                e.stopPropagation();
                input.value      = getLabel(item);
                input.dataset.id = getId(item);
                sugestoesDiv.innerHTML = "";
            });

            sugestoesDiv.appendChild(div);
        });
    });

    // fecha sugestões ao clicar fora
    document.addEventListener("click", (e) => {
        if (!e.target.closest(`#${input.id}`) &&
            !e.target.closest(`#${sugestoesDiv.id}`)) {
            sugestoesDiv.innerHTML = "";
        }
    });
}

// ─── Carregar empréstimos ─────────────────────────────────────────────────────
async function carregarEmprestimos() {
    try {
        const res   = await fetch(API_URL);
        const lista = await res.json();
        emprestimosCache = lista;
        renderizarTabela(lista);
    } catch (err) {
        console.error("Erro ao carregar empréstimos:", err);
        mostrarToast("Não foi possível conectar ao servidor.", "erro");
    }
}

// ─── Filtros ──────────────────────────────────────────────────────────────────
function aplicarFiltros() {
    const termo  = document.getElementById("search_emprestimo").value.toLowerCase();
    const status = document.getElementById("filtro-status").value;

    const filtrados = emprestimosCache.filter(emp => {
        const bateTexto = !termo ||
            emp.nomeUsuario.toLowerCase().includes(termo) ||
            emp.tituloLivro.toLowerCase().includes(termo);
        const bateStatus = !status || emp.status === status;
        return bateTexto && bateStatus;
    });

    renderizarTabela(filtrados);
}

function renderizarTabela(lista) {
    const tbody = document.querySelector("table tbody");
    tbody.innerHTML = "";

    if (lista.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#888;padding:20px;">Nenhum empréstimo cadastrado.</td></tr>`;
        return;
    }

    lista.forEach(emp => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${emp.nomeUsuario}</td>
            <td>${emp.tituloLivro}</td>
            <td>${formatarData(emp.dataEmprestimo)}</td>
            <td>${formatarData(emp.dataDevolucao)}</td>
            <td class="status ${classeStatus(emp.status)}">${emp.status}</td>
            <td class="acoes">
                <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>
                <div class="menu-acoes">
                    <div class="item" onclick="abrirEdicao(${emp.id})">Editar</div>
                    <div class="item" onclick="registrarDevolucao(${emp.id})">Registrar Devolução</div>
                    <div class="item" onclick="excluirEmprestimo(${emp.id})">Excluir</div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ─── Modal ────────────────────────────────────────────────────────────────────
function abrirModal() {
    modoEdicao = false;
    idEditando  = null;
    document.getElementById("modal-titulo").textContent = "Registrar Empréstimo";
    document.getElementById("btn-submit").textContent   = "Adicionar";
    document.getElementById("formEmprestimo").reset();
    inputUsuario.dataset.id = "";
    inputLivro.dataset.id   = "";
    document.getElementById("modal").style.display = "flex";
}

function fecharModal() {
    document.getElementById("modal").style.display = "none";
    sugestoesUsuario.innerHTML = "";
    sugestoesLivro.innerHTML   = "";
}

// ─── Criar empréstimo ─────────────────────────────────────────────────────────
async function criarEmprestimo() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Empréstimo registrado com sucesso!");
            fecharModal();
            carregarEmprestimos();
        } else {
            const err = await res.json();
            mostrarToast("Erro: " + err.erro, "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Editar ───────────────────────────────────────────────────────────────────
async function abrirEdicao(id) {
    fecharMenus();
    try {
        const res = await fetch(`${API_URL}?id=${id}`);
        const emp = await res.json();

        modoEdicao = true;
        idEditando  = id;

        document.getElementById("modal-titulo").textContent = "Editar Empréstimo";
        document.getElementById("btn-submit").textContent   = "Salvar";

        // usuário
        inputUsuario.value      = emp.nomeUsuario;
        inputUsuario.dataset.id = emp.idUsuario;

        // livro
        inputLivro.value      = emp.tituloLivro;
        inputLivro.dataset.id = emp.idLivro;

        document.getElementById("input-data-emp").value = emp.dataEmprestimo;
        document.getElementById("input-data-dev").value = emp.dataDevolucao;

        document.getElementById("modal").style.display = "flex";
    } catch {
        mostrarToast("Erro ao carregar dados para edição.", "erro");
    }
}

async function salvarEdicao() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(`${API_URL}?id=${idEditando}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Empréstimo atualizado!");
            fecharModal();
            carregarEmprestimos();
        } else {
            const err = await res.json();
            mostrarToast("Erro: " + err.erro, "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Devolver ─────────────────────────────────────────────────────────────────
async function registrarDevolucao(id) {
    fecharMenus();
    if (!confirm("Confirmar devolução deste empréstimo?")) return;

    try {
        const res = await fetch(`${API_URL}/devolver?id=${id}`, { method: "PUT" });
        if (res.ok) {
            mostrarToast("Devolução registrada!");
            carregarEmprestimos();
        } else {
            mostrarToast("Erro ao registrar devolução.", "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Excluir ──────────────────────────────────────────────────────────────────
async function excluirEmprestimo(id) {
    fecharMenus();
    if (!confirm("Tem certeza que deseja excluir este empréstimo?")) return;

    try {
        const res = await fetch(`${API_URL}?id=${id}`, { method: "DELETE" });
        if (res.ok) {
            mostrarToast("Empréstimo excluído.");
            carregarEmprestimos();
        } else {
            mostrarToast("Erro ao excluir.", "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Menu ─────────────────────────────────────────────────────────────────────
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

// ─── Helpers ──────────────────────────────────────────────────────────────────
function lerFormulario() {
    const idUsuario      = inputUsuario.dataset.id;
    const idLivro        = inputLivro.dataset.id;
    const dataEmprestimo = document.getElementById("input-data-emp").value;
    const dataDevolucao  = document.getElementById("input-data-dev").value;

    if (!idUsuario) {
        mostrarToast("Selecione um usuário da lista de sugestões.", "erro");
        return null;
    }
    if (!idLivro) {
        mostrarToast("Selecione um livro da lista de sugestões.", "erro");
        return null;
    }
    if (!dataEmprestimo || !dataDevolucao) {
        mostrarToast("Preencha as datas.", "erro");
        return null;
    }

    return {
        idUsuario: parseInt(idUsuario),
        idLivro:   parseInt(idLivro),
        dataEmprestimo,
        dataDevolucao
    };
}

function classeStatus(status) {
    if (status === "Atrasado")  return "atrasado";
    if (status === "Devolvido") return "devolvido";
    return "aberto";
}

function formatarData(iso) {
    if (!iso) return "-";
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
}

function mostrarToast(msg, tipo = "sucesso") {
    const toast = document.getElementById("toast");
    toast.textContent   = msg;
    toast.className     = `toast ${tipo}`;
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 3000);
}