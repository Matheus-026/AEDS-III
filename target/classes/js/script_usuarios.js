// ─── Configuração da API ──────────────────────────────────────────────────────
const API_Usuarios = "http://localhost:8080/api/usuarios";

// Cache de usuarios
let UsuariosCache = [];

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
let idEditando = null;

// ─── Ao carregar a página ─────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", async () => {
    await carregarUsuarios();

    const form = document.getElementById("formUsuario");
    if (form) {
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            if (modoEdicao) {
                await salvarEdicao();
            } else {
                await criarUsuario();
            }
        });
    }
});

// ─── Buscar usuarios ────────────────────────────────────────────────────────────
async function carregarUsuarios() {
    try {
        const res = await fetch(API_Usuarios);
        if (!res.ok) throw new Error("Falha ao buscar dados");
        
        UsuariosCache = await res.json();
        renderizarTabela(UsuariosCache);
    } catch (err) {
        console.error("Erro ao carregar usuarios:", err);
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

function renderizarTabela(lista) {
    const tbody = document.querySelector("table tbody");
    if (!tbody) return;
    
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

// ─── Modal ───────────────────────────────────────────────────────────────────
function abrirModal() {
    modoEdicao = false;
    idEditando = null;
    
    document.getElementById("modal-titulo").textContent = "Registrar Usuário";
    document.getElementById("btn-submit").textContent   = "Adicionar";
    document.getElementById("formUsuario").reset();
    
    // O campo de senha deve ser obrigatório na criação
    document.getElementById("input-senha").required = true;
    
    document.getElementById("modal").style.display = "flex";
}

function fecharModal() {
    document.getElementById("modal").style.display = "none";
}

// ─── Criar usuario ────────────────────────────────────────────────────────────
async function criarUsuario() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(API_Usuarios, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Usuário registrado com sucesso!");
            fecharModal();
            await carregarUsuarios();
        } else {
            mostrarToast("Erro ao criar usuário.", "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Editar ──────────────────────────────────────────────────────────────────
async function abrirEdicao(id) {
    fecharMenus();
    try {
        // Busca os dados do usuário específico
        const res = await fetch(`${API_Usuarios}/${id}`);
        const user = await res.json();

        modoEdicao = true;
        idEditando = id;

        document.getElementById("modal-titulo").textContent = "Editar Usuário";
        document.getElementById("btn-submit").textContent   = "Salvar";

        // Preenche o formulário
        document.getElementById("input-nome").value = user.nome;
        document.getElementById("input-email").value = user.email;
        document.getElementById("input-tipo").value = user.tipo;
        
        // Na edição, a senha pode ser opcional (ou mantida se vazia no seu Java)
        document.getElementById("input-senha").value = "";
        document.getElementById("input-senha").required = false;

        document.getElementById("modal").style.display = "flex";
    } catch (err) {
        mostrarToast("Erro ao carregar dados para edição.", "erro");
    }
}

async function salvarEdicao() {
    const payload = lerFormulario();
    if (!payload) return;

    try {
        const res = await fetch(`${API_Usuarios}/${idEditando}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            mostrarToast("Usuário atualizado!");
            fecharModal();
            await carregarUsuarios();
        } else {
            mostrarToast("Erro ao atualizar usuário.", "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Excluir ────────────────────────────────────────────────────────────────
async function excluirUsuario(id) {
    fecharMenus();
    if (!confirm("Tem certeza que deseja excluir este usuário?")) return;

    try {
        const res = await fetch(`${API_Usuarios}/${id}`, { method: "DELETE" });
        if (res.ok) {
            mostrarToast("Usuário excluído.");
            await carregarUsuarios();
        } else {
            mostrarToast("Erro ao excluir.", "erro");
        }
    } catch {
        mostrarToast("Erro ao conectar ao servidor.", "erro");
    }
}

// ─── Menu Ações ───────────────────────────────────────────────────────────────
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

// ─── Helpers ─────────────────────────────────────────────────────────────────
function lerFormulario() {
    const nome = document.getElementById("input-nome").value.trim();
    const email = document.getElementById("input-email").value.trim();
    const tipo = document.getElementById("input-tipo").value.trim();
    const senha = document.getElementById("input-senha").value.trim();

    if (!nome || !email) {
        mostrarToast("Nome e E-mail são obrigatórios.", "erro");
        return null;
    }

    // Só valida senha se não estiver editando ou se o campo não estiver vazio
    if (!modoEdicao && !senha) {
        mostrarToast("Senha é obrigatória para novos usuários.", "erro");
        return null;
    }

    return {
        nome: nome,
        email: email,
        tipo: tipo || "Standard",
        senha: senha
    };
}

function mostrarToast(msg, tipo = "sucesso") {
    const toast = document.getElementById("toast");
    if (!toast) return;
    
    toast.textContent = msg;
    toast.className   = `toast ${tipo}`;
    toast.style.display = "block";
    
    setTimeout(() => { 
        toast.style.display = "none"; 
    }, 3000);
}