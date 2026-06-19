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

    // ─── SUBMIT (BUSCA REAL - filtros normais) ──────────────────────────────
    formPesquisa.addEventListener('submit', function(event) {
        event.preventDefault();

        const formData = new FormData(formPesquisa);
        const dados = Object.fromEntries(formData.entries());

        console.log("Dados enviados do formulário:", dados);

        const url = new URL('http://localhost:8080/livros/busca');

        // Livros
        if (dados.titulo) url.searchParams.append('titulo', dados.titulo);
        if (dados.genero) url.searchParams.append('genero', dados.genero);
        if (dados.preco) {
            url.searchParams.append('min', -1);
            url.searchParams.append('max', dados.preco);
        }

        // Autores
        if (dados.nomeAutor) url.searchParams.append('autorNome', dados.nomeAutor);
        if (dados.telefoneAutor) url.searchParams.append('autorTelefone', dados.telefoneAutor);

        // Usuários
        if (dados.nomeUsuario) url.searchParams.append('usuarioNome', dados.nomeUsuario);
        if (dados.emailUsuario) url.searchParams.append('usuarioEmail', dados.emailUsuario);
        if (dados.statusUsuario) url.searchParams.append('usuarioStatus', dados.statusUsuario);

        // Empréstimos
        if (dados.livroEmprestado) url.searchParams.append('emprestimoLivro', dados.livroEmprestado);
        if (dados.usuarioEmprestimo) url.searchParams.append('emprestimoUsuario', dados.usuarioEmprestimo);
        if (dados.dataInicial) url.searchParams.append('dataInicial', dados.dataInicial);
        if (dados.dataFinal) url.searchParams.append('dataFinal', dados.dataFinal);

        console.log("URL gerada para o fetch:", url.toString());

        fetch(url.toString())
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
                    const statusEmprestimo = livro.usuarioEmprestimo ? `Emprestado para ${livro.usuarioEmprestimo}` : "Disponível";

                    html += `
                        <div class="resultado-item">
                            <strong>${livro.titulo}</strong>
                            <strong>Preço:</strong> R$ ${livro.preco}<br>
                            <strong>Gêneros:</strong> ${generos}<br>
                            <strong>Status:</strong> ${statusEmprestimo}
                        </div>
                    `;
                });

                divResultados.innerHTML = html;
            })
            .catch(error => {
                console.error("Erro na busca:", error);
                divResultados.innerHTML = "<p>Erro ao buscar livros</p>";
            });
    });

    // ═══════════════════════════════════════════════════════════════════════
    // SEÇÃO: BUSCA POR PADRÃO (KMP / Boyer-Moore)
    // Campos reais do modelo: titulo, autor, genero (não há "resumo").
    // ═══════════════════════════════════════════════════════════════════════

    const btnBuscaPadrao = document.getElementById('btn-buscar-padrao');

    if (btnBuscaPadrao) {
        btnBuscaPadrao.addEventListener('click', async () => {

            const padrao    = document.getElementById('input-padrao').value.trim();
            const algoritmo = document.getElementById('sel-algoritmo').value;
            const campo     = document.getElementById('sel-campo-padrao').value;

            if (!padrao) {
                divResultados.innerHTML = `<p class="erro-busca">Digite um padrão para buscar.</p>`;
                return;
            }

            divResultados.innerHTML = `<p>Buscando...</p>`;

            const url = new URL('http://localhost:8080/livros/busca-padrao');
            url.searchParams.append('padrao', padrao);
            url.searchParams.append('algoritmo', algoritmo);
            url.searchParams.append('campo', campo);

            try {
                const res  = await fetch(url.toString());
                const data = await res.json();

                if (!res.ok) {
                    divResultados.innerHTML = `<p class="erro-busca">${data.erro || 'Erro na busca.'}</p>`;
                    return;
                }

                if (!data.resultados || data.resultados.length === 0) {
                    divResultados.innerHTML = `
                        <p>Nenhum resultado encontrado para "<strong>${escaparTexto(padrao)}</strong>"
                        usando ${data.algoritmo}.</p>`;
                    return;
                }

                const infoTopo = `
                    <div class="info-busca-padrao">
                        <strong>${data.totalResultados}</strong> resultado(s) para
                        "<strong>${escaparTexto(padrao)}</strong>"
                        <span class="badge-algoritmo">${data.algoritmo}</span>
                    </div>`;

                const cards = data.resultados.map(livro => {
                    const generos  = livro.generos ? livro.generos.join(", ") : "Sem gênero";
                    const tituloHL = destacarPadrao(livro.titulo || "", padrao);
                    const autorHL  = destacarPadrao(livro.nomeAutor || "", padrao);
                    const generoHL = destacarPadrao(generos, padrao);

                    return `
                        <div class="resultado-item">
                            <strong>${tituloHL}</strong>
                            <span class="ocorrencias-tag">${livro.totalOcorrencias} ocorrência(s)</span><br>
                            <strong>Autor:</strong> ${autorHL}<br>
                            <strong>Preço:</strong> R$ ${livro.preco}<br>
                            <strong>Gêneros:</strong> ${generoHL}<br>
                            <strong>Publicação:</strong> ${escaparTexto(livro.dataPublicacao || "Não informada")}
                        </div>`;
                }).join("");

                divResultados.innerHTML = infoTopo + cards;

            } catch (err) {
                console.error("Erro na busca por padrão:", err);
                divResultados.innerHTML = `<p class="erro-busca">Erro ao conectar ao servidor.</p>`;
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    function escaparTexto(str) {
        if (!str) return "";
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");
    }

    function destacarPadrao(texto, padrao) {
        if (!texto || !padrao) return escaparTexto(texto);
        const textoEscapado  = escaparTexto(texto);
        const padraoEscapado = escaparTexto(padrao).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
        const re = new RegExp(padraoEscapado, "gi");
        return textoEscapado.replace(re, m => `<mark>${m}</mark>`);
    }
});