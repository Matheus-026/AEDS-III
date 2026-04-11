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


// ─── DOM carregado ───────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {

    const formPesquisa = document.querySelector('.pesquisa-form');
    const btnLimpar = document.querySelector('.btn-limpar');
    const btnCancelar = document.querySelector('.btn-cancelar');
    const inputPreco = document.querySelector('input[type="range"]');
    const displayPreco = document.getElementById('valorPrecoAtual');
    const divResultados = document.getElementById("resultados");

    // ─── Slider de preço ─────────────────────────────────────────────────────
    if (inputPreco && displayPreco) {
        inputPreco.addEventListener('input', function() {
            displayPreco.textContent = `R$ ${this.value}`;
        });
    }

    // ─── Botão limpar ────────────────────────────────────────────────────────
    if (btnLimpar) {
        btnLimpar.addEventListener('click', function() {
            formPesquisa.reset();

            if (inputPreco && displayPreco) {
                displayPreco.textContent = `R$ ${inputPreco.value}`;
            }

            divResultados.innerHTML = "";
        });
    }

    // ─── Botão cancelar ──────────────────────────────────────────────────────
    if (btnCancelar) {
        btnCancelar.addEventListener('click', function() {
            window.location.reload();
        });
    }

    // ─── SUBMIT (BUSCA REAL) ─────────────────────────────────────────────────
    formPesquisa.addEventListener('submit', function(event) {
        event.preventDefault();

        const formData = new FormData(formPesquisa);
        const dados = Object.fromEntries(formData.entries());

        console.log("Dados enviados:", dados);

        // Monta URL para o backend
        const url = `http://localhost:8080/livros/busca?` +
            `titulo=${encodeURIComponent(dados.titulo || "")}&` +
            `genero=${encodeURIComponent(dados.genero || "")}&` +
            `min=-1&` +
            `max=${Number(dados.preco) || -1}`;

        fetch(url)
            .then(response => response.json())
            .then(data => {

                console.log("Resposta do backend:", data);

                if (!data || data.length === 0) {
                    divResultados.innerHTML = "<p>Nenhum resultado encontrado</p>";
                    return;
                }

                let html = "";

                data.forEach(livro => {
                    const generos = livro.generos ? livro.generos.join(", ") : "Sem gênero";

                    html += `
                        <div class="resultado-item">
                            <strong>${livro.titulo}</strong><br>
                            Preço: R$ ${livro.preco}<br>
                            Gêneros: ${generos}
                        </div>
                    <hr>
                 `;
                });

                divResultados.innerHTML = html;
            })
            .catch(error => {
                console.error("Erro na busca:", error);
                divResultados.innerHTML = "<p>Erro ao buscar livros</p>";
            });
    });
});