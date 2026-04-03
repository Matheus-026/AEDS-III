// ─── Configuração da API ──────────────────────────────────────────────────────
const API_URL = "http://localhost:8080/api/emprestimos";

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

// ─── Ao carregar a página ─────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
    carregarEmprestimos();

    document.getElementById("formEmprestimo").addEventListener("submit", async (e) => {
        e.preventDefault();
        if (modoEdicao) {
            await salvarEdicao();
        } else {
            await criarEmprestimo();
        }
    });
});

// ─── Carregar e renderizar tabela ─────────────────────────────────────────────
async function carregarEmprestimos() {
    try {
        const res  = await fetch(API_URL);
        const lista = await res.json();
        renderizarTabela(lista);
    } catch (err) {
        console.error("Erro ao carregar empréstimos:", err);
        mostrarToast("Não foi possível conectar ao servidor.", "erro");
    }
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
                    <div class="item" onclick="abrirEdicao(${emp.id})">
                        <span class="icon editar">✏️</span>
                        Editar
                    </div>
                    <div class="item" onclick="registrarDevolucao(${emp.id})">
                        <span class="icon devolver">➤</span>
                        Registrar Devolução
                    </div>
                    <div class="item" onclick="excluirEmprestimo(${emp.id})">
                        <span class="icon detalhes">🗑️</span>
                        Excluir
                    </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ─── Abrir / Fechar Modal ─────────────────────────────────────────────────────
function abrirModal() {
    modoEdicao = false;
    idEditando  = null;
    document.getElementById("modal-titulo").textContent = "Registrar Empréstimo";
    document.getElementById("btn-submit").textContent   = "Adicionar";
    document.getElementById("formEmprestimo").reset();
    document.getElementById("modal").style.display = "flex";
}

function fecharModal() {
    document.getElementById("modal").style.display = "none";
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

// ─── Editar empréstimo ────────────────────────────────────────────────────────
async function abrirEdicao(id) {
    fecharMenus();
    try {
        const res = await fetch(`${API_URL}?id=${id}`);
        const emp = await res.json();

        modoEdicao = true;
        idEditando  = id;

        document.getElementById("modal-titulo").textContent = "Editar Empréstimo";
        document.getElementById("btn-submit").textContent   = "Salvar";
        document.getElementById("input-usuario").value      = emp.idUsuario;
        document.getElementById("input-livro").value        = emp.idLivro;
        document.getElementById("input-data-emp").value     = emp.dataEmprestimo;
        document.getElementById("input-data-dev").value     = emp.dataDevolucao;

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

// ─── Registrar devolução ──────────────────────────────────────────────────────
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

// ─── Excluir empréstimo ───────────────────────────────────────────────────────
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

// ─── Menu de ações (⋮) ───────────────────────────────────────────────────────
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

// ─── Helpers ─────────────────────────────────────────────────────────────────
function lerFormulario() {
    const idUsuario      = document.getElementById("input-usuario").value.trim();
    const idLivro        = document.getElementById("input-livro").value.trim();
    const dataEmprestimo = document.getElementById("input-data-emp").value;
    const dataDevolucao  = document.getElementById("input-data-dev").value;

    if (!idUsuario || !idLivro || !dataEmprestimo || !dataDevolucao) {
        mostrarToast("Preencha todos os campos.", "erro");
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

// "2026-04-20" → "20/04/2026"
function formatarData(iso) {
    if (!iso) return "-";
    const [y, m, d] = iso.split("-");
    return `${d}/${m}/${y}`;
}

// ─── Toast de feedback ────────────────────────────────────────────────────────
function mostrarToast(msg, tipo = "sucesso") {
    const toast = document.getElementById("toast");
    toast.textContent = msg;
    toast.className   = `toast ${tipo}`;
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 3000);
}