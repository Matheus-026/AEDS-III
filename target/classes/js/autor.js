const API_URL = "http://localhost:8080/api/autores";

const modal = document.getElementById("modalAutor");
const form = document.getElementById("formAutor");
const tbody = document.querySelector("tbody");

let modoEdicao = false;
let idEditando = null;

// MODAL
function abrirModal() {
    modal.style.display = "flex";
}

function fecharModal() {
    modal.style.display = "none";
    form.reset();
    modoEdicao = false;
    idEditando = null;
}

// LISTAR
async function carregarAutores() {
    const res = await fetch(API_URL);
    const autores = await res.json();

    tbody.innerHTML = "";

    autores.forEach(a => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>${a.nome}</td>
            <td>${a.telefone || "-"}</td>
			<td>
			    <span>
			        ${resumirTexto(a.biografia, 80)}
			        ${
			            a.biografia && a.biografia.length > 80
			            ? `<a href="#" 
			                 onclick="verBiografia('${encodeURIComponent(a.biografia)}'); return false;" 
			                 style="margin-left:6px; font-size:13px; color:var(--orange); white-space:nowrap;">
			                 Ver mais
			               </a>`
			            : ""
			        }
			    </span>
			</td>
            <td class="acoes">
                <button class="acao-btn" onclick="toggleMenu(this)">⋮</button>
                <div class="menu-acoes">
                    <div class="item" onclick="abrirEdicao(${a.id})"> Editar</div>
                    <div class="item" onclick="excluirAutor(${a.id})"> Excluir</div>
                </div>
            </td>
        `;

        tbody.appendChild(tr);
    });
}

// EDITAR
async function abrirEdicao(id) {
    const res = await fetch(`${API_URL}/${id}`);
    const a = await res.json();

    modoEdicao = true;
    idEditando = id;

    document.getElementById("nome").value = a.nome;
    document.getElementById("telefone").value = a.telefone;
    document.getElementById("biografia").value = a.biografia;

    abrirModal();
}

// DELETE
async function excluirAutor(id) {
    if (!confirm("Excluir autor?")) return;

    await fetch(`${API_URL}/${id}`, { method: "DELETE" });

    carregarAutores();
}

// SALVAR
form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const autor = {
        nome: document.getElementById("nome").value,
        telefone: document.getElementById("telefone").value,
        biografia: document.getElementById("biografia").value
    };

    if (modoEdicao) {
        await fetch(`${API_URL}/${idEditando}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(autor)
        });
    } else {
        await fetch(API_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(autor)
        });
    }

    fecharModal();
    carregarAutores();
});

// MENU
function toggleMenu(btn) {
    const menu = btn.nextElementSibling;

    document.querySelectorAll(".menu-acoes").forEach(m => {
        if (m !== menu) m.style.display = "none";
    });

    menu.style.display = menu.style.display === "block" ? "none" : "block";
}

document.addEventListener("click", (e) => {
    if (!e.target.closest(".acoes")) {
        document.querySelectorAll(".menu-acoes").forEach(m => m.style.display = "none");
    }
});

function resumirTexto(texto, limite) {
    if (!texto) return "-";

    if (texto.length <= limite) return texto;

    return texto.substring(0, limite) + "...";
}

function verBiografia(textoCodificado) {
    const texto = decodeURIComponent(textoCodificado);

    document.getElementById("bio-texto").innerText = texto;
    document.getElementById("modalBio").style.display = "flex";
}

function fecharModalBio() {
    document.getElementById("modalBio").style.display = "none";
}

// INIT
window.onload = carregarAutores;