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

function abrirModal() {
    document.getElementById("modal").style.display = "flex";
}

function fecharModal() {
    document.getElementById("modal").style.display = "none";
}

document.getElementById("formEmprestimo").addEventListener("submit", function (e) {
    e.preventDefault();
    alert("Empréstimo registrado (backend depois)");
    fecharModal();
});

function toggleMenu(botao) {
    const menu = botao.nextElementSibling;

    // fecha outros
    document.querySelectorAll('.menu-acoes').forEach(m => {
        if (m !== menu) m.style.display = 'none';
    });

    menu.style.display = menu.style.display === 'block' ? 'none' : 'block';
}

// fecha ao clicar fora
document.addEventListener('click', function (e) {
    if (!e.target.closest('.acoes')) {
        document.querySelectorAll('.menu-acoes').forEach(m => {
            m.style.display = 'none';
        });
    }
});

/* AÇÕES */
function editar() {
    alert("Editar empréstimo");
}

function devolver() {
    alert("Devolução registrada");
}

function detalhes() {
    alert("Abrir detalhes");
}