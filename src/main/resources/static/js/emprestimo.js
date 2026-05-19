// ─── Configuração da API ──────────────────────────────────────────────────────
const API_URL      = "http://localhost:8080/api/emprestimos";
const API_LIVROS   = "http://localhost:8080/api/livros";
const API_USUARIOS = "http://localhost:8080/api/usuarios";

// caches
let livrosCache      = [];
let emprestimosCache = [];
let usuariosCache    = [];

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

// ─── Aba ativa ────────────────────────────────────────────────────────────────
// "todos" | "porUsuario" | "porLivro"
let abaAtiva = "todos";

// ─── Elementos de autocomplete ────────────────────────────────────────────────
const inputUsuario     = document.getElementById("input-usuario-nome");
const sugestoesUsuario = document.getElementById("lista-usuarios");
const inputLivro       = document.getElementById("input-livro-nome");
const sugestoesLivro   = document.getElementById("lista-livros");

// ─── Ao carregar a página ─────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", async () => {
    await Promise.all([carregarLivros(), carregarUsuarios()]);
    await carregarEmprestimos();

    // busca geral
    document.getElementById("search_emprestimo")
        .addEventListener("input", aplicarFiltros);
    document.getElementById("filtro-status")
        ?.addEventListener("change", aplicarFiltros);

    // filtros N:N
    document.getElementById("filtro-usuario")
        ?.addEventListener("change", consultarPorUsuario);
    document.getElementById("filtro-livro")
        ?.addEventListener("change", consultarPorLivro);

    // abas
    document.querySelectorAll(".aba-btn").forEach(btn => {
        btn.addEventListener("click", () => trocarAba(btn.dataset.aba));
    });

    // autocomplete modal
    configurarAutocomplete(
        inputUsuario, sugestoesUsuario,
        () => usuariosCache, u => u.nome, u => u.id
    );
    configurarAutocomplete(
        inputLivro, sugestoesLivro,
        () => livrosCache, l => l.titulo, l => l.id
    );

    // ao selecionar usuário no modal, verifica livros já ativos
    inputUsuario.addEventListener("change", atualizarIndicadoresLivro);
    inputLivro.addEventListener("change",   atualizarIndicadoresLivro);

    // submit do formulário
    document.getElementById("formEmprestimo").addEventListener("submit", async (e) => {
        e.preventDefault();
        if (modoEdicao) await salvarEdicao();
        else            await criarEmprestimo();
    });

    // popular selects de filtro N:N
    popularFiltroUsuario();
    popularFiltroLivro();
});

// ─── Abas ─────────────────────────────────────────────────────────────────────
function trocarAba(aba) {
    abaAtiva = aba;

    document.querySelectorAll(".aba-btn").forEach(btn => {
        btn.classList.toggle("ativa", btn.dataset.aba === aba);
    });

    document.getElementById("painel-todos").style.display     = aba === "todos"      ? "" : "none";
    document.getElementById("painel-usuario").style.display   = aba === "porUsuario" ? "" : "none";
    document.getElementById("painel-livro").style.display     = aba === "porLivro"   ? "" : "none";
}

// ─── Popular selects dos filtros N:N ─────────────────────────────────────────
function popularFiltroUsuario() {
    const sel = document.getElementById("filtro-usuario");
    if (!sel) return;
    sel.innerHTML = `<option value="">Selecione um usuário...</option>`;
    usuariosCache.forEach(u => {
        sel.innerHTML += `<option value="${u.id}">${u.nome}</option>`;
    });
}

function popularFiltroLivro() {
    const sel = document.getElementById("filtro-livro");
    if (!sel) return;
    sel.innerHTML = `<option value="">Selecione um livro...</option>`;
    livrosCache.forEach(l => {
        sel.innerHTML += `<option value="${l.id}">${l.titulo}</option>`;
    });
}

// ─── Consulta N:N — por usuário ───────────────────────────────────────────────
async function consultarPorUsuario() {
    const sel   = document.getElementById("filtro-usuario");
    const id    = sel?.value;
    const tbody = document.getElementById("tbody-usuario");
    if (!tbody) return;

    if (!id) {
        tbody.innerHTML = `<tr><td colspan="5" class="vazio">Selecione um usuário para ver seus empréstimos.</td></tr>`;
        document.getElementById("info-usuario").textContent = "";
        return;
    }

    const usuario = usuariosCache.find(u => u.id == id);
    document.getElementById("info-usuario").textContent =
        usuario ? `Empréstimos de: ${usuario.nome}` : "";

    try {
        const res = await fetch(`${API_URL}/usuario/${id}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const lista = await res.json();

        if (!lista.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="vazio">Nenhum empréstimo encontrado para este usuário.</td></tr>`;
            return;
        }

        tbody.innerHTML = lista.map(emp => `
            <tr>
                <td>${emp.tituloLivro || `Livro #${emp.idLivro}`}</td>
                <td>${formatarData(emp.dataEmprestimo)}</td>
                <td>${formatarData(emp.dataDevolucao)}</td>
                <td class="status ${classeStatus(emp.status)}">${emp.status}</td>
                <td class="acoes">
                    <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>
                    <div class="menu-acoes">
                        <div class="item" onclick="registrarDevolucao(${emp.id})">Registrar Devolução</div>
                        <div class="item" onclick="excluirEmprestimo(${emp.id})">Excluir</div>
                    </div>
                </td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="vazio erro">Erro ao buscar empréstimos.</td></tr>`;
        console.error(err);
    }
}

// ─── Consulta N:N — por livro ─────────────────────────────────────────────────
async function consultarPorLivro() {
    const sel   = document.getElementById("filtro-livro");
    const id    = sel?.value;
    const tbody = document.getElementById("tbody-livro");
    if (!tbody) return;

    if (!id) {
        tbody.innerHTML = `<tr><td colspan="5" class="vazio">Selecione um livro para ver seu histórico.</td></tr>`;
        document.getElementById("info-livro").textContent = "";
        return;
    }

    const livro = livrosCache.find(l => l.id == id);
    document.getElementById("info-livro").textContent =
        livro ? `Histórico de: ${livro.titulo}` : "";

    try {
        const res = await fetch(`${API_URL}/livro/${id}`);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const lista = await res.json();

        if (!lista.length) {
            tbody.innerHTML = `<tr><td colspan="5" class="vazio">Nenhum empréstimo encontrado para este livro.</td></tr>`;
            return;
        }

        tbody.innerHTML = lista.map(emp => `
            <tr>
                <td>${emp.nomeUsuario || `Usuário #${emp.idUsuario}`}</td>
                <td>${formatarData(emp.dataEmprestimo)}</td>
                <td>${formatarData(emp.dataDevolucao)}</td>
                <td class="status ${classeStatus(emp.status)}">${emp.status}</td>
                <td class="acoes">
                    <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>
                    <div class="menu-acoes">
                        <div class="item" onclick="registrarDevolucao(${emp.id})">Registrar Devolução</div>
                        <div class="item" onclick="excluirEmprestimo(${emp.id})">Excluir</div>
                    </div>
                </td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="vazio erro">Erro ao buscar empréstimos.</td></tr>`;
        console.error(err);
    }
}

// ─── Indicador visual — livro já ativo para o usuário ─────────────────────────
// Quando usuário E livro estão selecionados no modal, consulta o backend
// para verificar se já existe empréstimo ativo para esse par.
async function atualizarIndicadoresLivro() {
    const idUsuario = inputUsuario.dataset.id;
    const idLivro   = inputLivro.dataset.id;
    const aviso     = document.getElementById("aviso-par-ativo");
    if (!aviso) return;

    if (!idUsuario || !idLivro) {
        aviso.style.display = "none";
        return;
    }

    try {
        const res = await fetch(
            `${API_URL}/ativo?idUsuario=${idUsuario}&idLivro=${idLivro}`
        );

        if (res.ok) {
            const emp = await res.json();
            if (emp && emp.id) {
                // já existe empréstimo ativo para esse par
                aviso.style.display = "flex";
                aviso.innerHTML = `
                    ⚠️ Este usuário já tem este livro em aberto
                    (empréstimo #${emp.id}, devolução: ${formatarData(emp.dataDevolucao)}).
                    É necessário devolver antes de um novo empréstimo.
                `;
                document.getElementById("btn-submit").disabled = true;
            } else {
                aviso.style.display = "none";
                document.getElementById("btn-submit").disabled = false;
            }
        } else {
            // 404 = não encontrado = par livre
            aviso.style.display = "none";
            document.getElementById("btn-submit").disabled = false;
        }
    } catch {
        aviso.style.display = "none";
    }
}

// ─── Carregar dados ───────────────────────────────────────────────────────────
async function carregarLivros() {
    try {
        const res  = await fetch(API_LIVROS);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        livrosCache = Array.isArray(data) ? data : [];
    } catch (err) {
        console.error("Erro ao carregar livros:", err);
        mostrarToast("Erro ao carregar livros.", "erro");
    }
}

async function carregarUsuarios() {
    try {
        const res  = await fetch(API_USUARIOS);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        usuariosCache = Array.isArray(data) ? data : [];
    } catch (err) {
        console.error("Erro ao carregar usuários:", err);
        mostrarToast("Erro ao carregar usuários.", "erro");
    }
}

async function carregarEmprestimos() {
    try {
        const res = await fetch(API_URL);
        if (!res.ok) {
            let msg = `HTTP ${res.status}`;
            try { const c = await res.json(); if (c?.erro) msg = c.erro; } catch {}
            renderizarErro(msg);
            mostrarToast("Não foi possível carregar os empréstimos: " + msg, "erro");
            return;
        }
        const data = await res.json();
        if (!Array.isArray(data)) {
            renderizarErro("Resposta inválida do servidor.");
            return;
        }
        emprestimosCache = data;
        renderizarTabela(data);
    } catch (err) {
        console.error(err);
        renderizarErro("Não foi possível conectar ao servidor.");
        mostrarToast("Não foi possível conectar ao servidor.", "erro");
    }
}

// ─── Autocomplete genérico ────────────────────────────────────────────────────
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
                // dispara verificação de par ativo
                input.dispatchEvent(new Event("change"));
            });

            sugestoesDiv.appendChild(div);
        });
    });

    document.addEventListener("click", (e) => {
        if (!e.target.closest(`#${input.id}`) &&
            !e.target.closest(`#${sugestoesDiv.id}`)) {
            sugestoesDiv.innerHTML = "";
        }
    });
}

// ─── Filtros (aba "todos") ────────────────────────────────────────────────────
function aplicarFiltros() {
    const termo  = document.getElementById("search_emprestimo").value.toLowerCase();
    const status = document.getElementById("filtro-status")?.value || "";

    const filtrados = emprestimosCache.filter(emp => {
        const bateTexto  = !termo ||
            (emp.nomeUsuario || "").toLowerCase().includes(termo) ||
            (emp.tituloLivro || "").toLowerCase().includes(termo) ||
            (emp.status      || "").toLowerCase().includes(termo);
        const bateStatus = !status || emp.status === status;
        return bateTexto && bateStatus;
    });

    renderizarTabela(filtrados);
}

// ─── Renderizar tabela principal ──────────────────────────────────────────────
function renderizarTabela(lista) {
    const tbody = document.querySelector("#tabela-todos tbody");
    if (!tbody) return;
    tbody.innerHTML = "";

    if (!Array.isArray(lista) || lista.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="vazio">Nenhum empréstimo cadastrado.</td></tr>`;
        return;
    }

    lista.forEach(emp => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${emp.nomeUsuario  || "-"}</td>
            <td>${emp.tituloLivro  || "-"}</td>
            <td>${formatarData(emp.dataEmprestimo)}</td>
            <td>${formatarData(emp.dataDevolucao)}</td>
            <td class="status ${classeStatus(emp.status)}">${emp.status || "-"}</td>
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

function renderizarErro(msg) {
    const tbody = document.querySelector("#tabela-todos tbody");
    if (tbody) {
        tbody.innerHTML = `<tr><td colspan="6" class="vazio erro">⚠ ${msg}</td></tr>`;
    }
}

// ─── Modal ────────────────────────────────────────────────────────────────────
function abrirModal() {
    modoEdicao = false;
    idEditando  = null;
    document.getElementById("modal-titulo").textContent   = "Registrar Empréstimo";
    document.getElementById("btn-submit").textContent     = "Adicionar";
    document.getElementById("btn-submit").disabled        = false;
    document.getElementById("formEmprestimo").reset();
    inputUsuario.dataset.id = "";
    inputLivro.dataset.id   = "";
    const aviso = document.getElementById("aviso-par-ativo");
    if (aviso) aviso.style.display = "none";
    document.getElementById("modal").style.display = "flex";
}

function fecharModal() {
    document.getElementById("modal").style.display = "none";
    sugestoesUsuario.innerHTML = "";
    sugestoesLivro.innerHTML   = "";
    const aviso = document.getElementById("aviso-par-ativo");
    if (aviso) aviso.style.display = "none";
    document.getElementById("btn-submit").disabled = false;
}

// ─── Criar empréstimo ─────────────────────────────────────────────────────────
async function criarEmprestimo() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(API_URL, {
            method:  "POST",
            headers: { "Content-Type": "application/json" },
            body:    JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Empréstimo registrado com sucesso!");
            fecharModal();
            carregarEmprestimos();
        } else {
            const err = await res.json().catch(() => ({}));
            // mensagem amigável para par duplicado ativo
            const msg = err.erro || err.message || `HTTP ${res.status}`;
            if (msg.includes("empréstimo ativo")) {
                mostrarToast("Este livro já está emprestado para este usuário. Registre a devolução primeiro.", "erro");
            } else {
                mostrarToast("Erro: " + msg, "erro");
            }
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Editar ───────────────────────────────────────────────────────────────────
async function abrirEdicao(id) {
    fecharMenus();
    try {
        const res = await fetch(`${API_URL}/${id}`);
        if (!res.ok) { mostrarToast("Empréstimo não encontrado.", "erro"); return; }

        const emp = await res.json();

        modoEdicao = true;
        idEditando  = id;

        document.getElementById("modal-titulo").textContent = "Editar Empréstimo";
        document.getElementById("btn-submit").textContent   = "Salvar";
        document.getElementById("btn-submit").disabled      = false;

        inputUsuario.value      = emp.nomeUsuario || "";
        inputUsuario.dataset.id = emp.idUsuario;
        inputLivro.value        = emp.tituloLivro || "";
        inputLivro.dataset.id   = emp.idLivro;

        document.getElementById("input-data-emp").value = emp.dataEmprestimo || "";
        document.getElementById("input-data-dev").value = emp.dataDevolucao  || "";

        const aviso = document.getElementById("aviso-par-ativo");
        if (aviso) aviso.style.display = "none";

        document.getElementById("modal").style.display = "flex";
    } catch {
        mostrarToast("Erro ao carregar dados para edição.", "erro");
    }
}

async function salvarEdicao() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(`${API_URL}/${idEditando}`, {
            method:  "PUT",
            headers: { "Content-Type": "application/json" },
            body:    JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Empréstimo atualizado!");
            fecharModal();
            carregarEmprestimos();
        } else {
            const err = await res.json().catch(() => ({}));
            mostrarToast("Erro: " + (err.erro || `HTTP ${res.status}`), "erro");
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
        const res = await fetch(`${API_URL}/${id}/devolver`, { method: "PUT" });
        if (res.ok) {
            mostrarToast("Devolução registrada!");
            carregarEmprestimos();
            // atualiza painéis N:N se estiverem ativos
            if (abaAtiva === "porUsuario") consultarPorUsuario();
            if (abaAtiva === "porLivro")   consultarPorLivro();
        } else {
            const err = await res.json().catch(() => ({}));
            mostrarToast("Erro: " + (err.erro || `HTTP ${res.status}`), "erro");
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
        const res = await fetch(`${API_URL}/${id}`, { method: "DELETE" });
        if (res.ok) {
            mostrarToast("Empréstimo excluído.");
            carregarEmprestimos();
            if (abaAtiva === "porUsuario") consultarPorUsuario();
            if (abaAtiva === "porLivro")   consultarPorLivro();
        } else {
            const err = await res.json().catch(() => ({}));
            mostrarToast("Erro: " + (err.erro || `HTTP ${res.status}`), "erro");
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
        idUsuario:      parseInt(idUsuario),
        idLivro:        parseInt(idLivro),
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
    setTimeout(() => { toast.style.display = "none"; }, 3500);
}