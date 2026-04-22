// ─── API ──────────────────────────────────────────────────────────────────────
const API_Usuarios = "http://localhost:8080/api/usuarios";

let UsuariosCache = [];
let idEditando    = null;

// ─── Navbar hide on scroll ────────────────────────────────────────────────────
const navbar = document.querySelector('.navbar');
let ultimoScroll = 0;
window.addEventListener('scroll', () => {
    const scrollAtual = window.scrollY;
    navbar.classList.toggle('escondida', scrollAtual > ultimoScroll && scrollAtual > 200);
    ultimoScroll = scrollAtual;
});

// ─── Init ─────────────────────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", async () => {
    await carregarUsuarios();

    document.getElementById("formCriar").addEventListener("submit", async (e) => {
        e.preventDefault();
        await criarUsuario();
    });

    document.getElementById("formEditar").addEventListener("submit", async (e) => {
        e.preventDefault();
        await salvarEdicao();
    });

    // ── Filtros combinados ────────────────────────────────────────────────────
    function aplicarFiltros() {
        const termo = document.getElementById("search_usuario").value.toLowerCase();
        const tipo  = document.getElementById("filtro-tipo").value;

        const filtrados = UsuariosCache.filter(u => {
            const buscaOk = u.nome.toLowerCase().includes(termo)
                         || u.email.toLowerCase().includes(termo)
                         || u.tipo.toLowerCase().includes(termo);
            const tipoOk  = tipo === "" || u.tipo === tipo;
            return buscaOk && tipoOk;
        });

        renderizarTabela(filtrados);
    }

    document.getElementById("search_usuario").addEventListener("input", aplicarFiltros);
    document.getElementById("filtro-tipo").addEventListener("change", aplicarFiltros);
});

// ─── Carregar ─────────────────────────────────────────────────────────────────
async function carregarUsuarios() {
    try {
        const res = await fetch(API_Usuarios);
        if (!res.ok) throw new Error();
        UsuariosCache = await res.json();
        renderizarTabela(UsuariosCache);
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

function renderizarTabela(lista) {
    const tbody = document.querySelector("table tbody");
    tbody.innerHTML = "";

    if (lista.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:#888;padding:20px;">Nenhum usuário cadastrado.</td></tr>`;
        return;
    }

    lista.forEach(user => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${user.nome}</td>
            <td>${user.email}</td>
            <td>${user.tipo}</td>
            <td class="acoes">
                <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>
                <div class="menu-acoes" style="display:none;">
                    <div class="item" onclick="abrirEdicao(${user.id})">Editar</div>
                    <div class="item" onclick="excluirUsuario(${user.id})">Excluir</div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// ─── Modal Criar ──────────────────────────────────────────────────────────────
function abrirModal() {
    document.getElementById("formCriar").reset();
    document.getElementById("modal-criar").style.display = "flex";
}

function fecharModalCriar() {
    document.getElementById("modal-criar").style.display = "none";
}

// ─── Modal Editar ─────────────────────────────────────────────────────────────
async function abrirEdicao(id) {
    fecharMenus();
    try {
        const res  = await fetch(`${API_Usuarios}/${id}`);
        const user = await res.json();

        idEditando = id;

        document.getElementById("editar-nome").value        = user.nome;
        document.getElementById("editar-email").value       = user.email;
        document.getElementById("editar-tipo").value        = user.tipo;
        document.getElementById("editar-nova-senha").value  = "";
        document.getElementById("editar-senha-adm").value   = "";

        document.getElementById("modal-editar").style.display = "flex";
    } catch {
        mostrarToast("Erro ao carregar dados para edição.", "erro");
    }
}

function fecharModalEditar() {
    document.getElementById("modal-editar").style.display = "none";
    document.getElementById("editar-senha-adm").value = "";
    document.getElementById("editar-nova-senha").value = "";
}

// ─── Criar ────────────────────────────────────────────────────────────────────
async function criarUsuario() {
    const nome  = document.getElementById("criar-nome").value.trim();
    const email = document.getElementById("criar-email").value.trim();
    const tipo  = document.getElementById("criar-tipo").value;
    const senha = document.getElementById("criar-senha").value.trim();

    if (!nome || !email || !senha) {
        mostrarToast("Nome, e-mail e senha são obrigatórios.", "erro");
        return;
    }

    try {
        const res = await fetch(API_Usuarios, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ nome, email, senha, tipo })
        });

        if (res.ok) {
            mostrarToast("Usuário registrado com sucesso!");
            fecharModalCriar();
            await carregarUsuarios();
        } else {
            const msg = await res.text();
            mostrarToast("Erro: " + msg, "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Salvar Edição ────────────────────────────────────────────────────────────
async function salvarEdicao() {
    const nome      = document.getElementById("editar-nome").value.trim();
    const email     = document.getElementById("editar-email").value.trim();
    const tipo      = document.getElementById("editar-tipo").value;
    const novaSenha = document.getElementById("editar-nova-senha").value.trim();
    const senhaAdm  = document.getElementById("editar-senha-adm").value.trim();

    if (!nome || !email) {
        mostrarToast("Nome e e-mail são obrigatórios.", "erro");
        return;
    }

    if (!senhaAdm) {
        mostrarToast("Confirme com a senha do administrador.", "erro");
        return;
    }

    try {
        // ── 1. Verifica a senha do admin antes de qualquer alteração ──────────
        const verificacao = await fetch(`${API_Usuarios}/verificar-admin`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ senha: senhaAdm })
        });

        if (!verificacao.ok) {
            mostrarToast("Senha do administrador incorreta.", "erro");
            document.getElementById("editar-senha-adm").value = "";
            document.getElementById("editar-senha-adm").focus();
            return;
        }

        // ── 2. Senha correta — envia a atualização ────────────────────────────
        const payload = { nome, email, tipo };
        if (novaSenha) payload.novaSenha = novaSenha;

        const res = await fetch(`${API_Usuarios}/${idEditando}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Usuário atualizado!");
            fecharModalEditar();
            await carregarUsuarios();
        } else {
            const msg = await res.text();
            mostrarToast("Erro: " + msg, "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Excluir ──────────────────────────────────────────────────────────────────
async function excluirUsuario(id) {
    fecharMenus();
    if (!confirm("Tem certeza que deseja excluir este usuário?")) return;

    try {
        const res = await fetch(`${API_Usuarios}/${id}`, { method: "DELETE" });

        if (res.ok) {
            mostrarToast("Usuário excluído.");
            await carregarUsuarios();
        } else if (res.status === 409) {
            const msg = await res.text();
            mostrarToast(msg, "erro");
        } else {
            mostrarToast("Erro ao excluir usuário.", "erro");
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
    menu.style.display = (menu.style.display === 'none' || menu.style.display === '') ? 'block' : 'none';
}

function fecharMenus() {
    document.querySelectorAll('.menu-acoes').forEach(m => m.style.display = 'none');
}

document.addEventListener('click', (e) => {
    if (!e.target.closest('.acoes')) fecharMenus();
});

// ─── Toast ────────────────────────────────────────────────────────────────────
function mostrarToast(msg, tipo = "sucesso") {
    const toast = document.getElementById("toast");
    if (!toast) return;
    toast.textContent   = msg;
    toast.className     = `toast ${tipo}`;
    toast.style.display = "block";
    setTimeout(() => { toast.style.display = "none"; }, 4000);
}