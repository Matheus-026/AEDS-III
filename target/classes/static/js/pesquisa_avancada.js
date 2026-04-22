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

        console.log("Dados enviados do formulário:", dados);

        // 1. Monta a URL inteligente
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

        // 2. Faz a requisição para o backend
        fetch(url.toString())
            .then(response => response.json())
            .then(data => {
                console.log("Resposta do backend:", data);

                if (!data || data.length === 0) {
                    divResultados.innerHTML = "<p>Nenhum resultado encontrado</p>";
                    return;
                }

                let html = "";

                // 3. Monta os cards com os novos campos (Autor e Status)
                data.forEach(livro => {
                    const generos = livro.generos ? livro.generos.join(", ") : "Sem gênero";
                    const autor = livro.nomeAutor ? livro.nomeAutor : "Não informado";
                    const statusEmprestimo = livro.usuarioEmprestimo ? `Emprestado para ${livro.usuarioEmprestimo}` : "Disponível";

                    // Note que usei a classe 'resultado-item' para pegar o estilo de caixinha do CSS
                    html += `
                        <div class="resultado-item">
                            <strong>${livro.titulo}</strong>
                            <strong>Autor:</strong> ${autor}<br>
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

    // ─── Monta URL para o backend com TODOS os filtros ─────────────────────
        const url = new URL('http://localhost:8080/livros/busca');
        
        // Adiciona apenas os campos que foram preenchidos (ignora os vazios)
        if (dados.titulo) url.searchParams.append('titulo', dados.titulo);
        if (dados.genero) url.searchParams.append('genero', dados.genero);
        if (dados.preco) {
            url.searchParams.append('min', -1); // Supondo que você quer manter o min fixo como no seu código original
            url.searchParams.append('max', dados.preco);
        }
    
        if (dados.nomeAutor) url.searchParams.append('autorNome', dados.nomeAutor);
        if (dados.telefoneAutor) url.searchParams.append('autorTelefone', dados.telefoneAutor);
        
        if (dados.nomeUsuario) url.searchParams.append('usuarioNome', dados.nomeUsuario);
        if (dados.emailUsuario) url.searchParams.append('usuarioEmail', dados.emailUsuario);
        if (dados.statusUsuario) url.searchParams.append('usuarioStatus', dados.statusUsuario);
        
        if (dados.livroEmprestado) url.searchParams.append('emprestimoLivro', dados.livroEmprestado);
        if (dados.usuarioEmprestimo) url.searchParams.append('emprestimoUsuario', dados.usuarioEmprestimo);
        if (dados.dataInicial) url.searchParams.append('dataInicial', dados.dataInicial);
        if (dados.dataFinal) url.searchParams.append('dataFinal', dados.dataFinal);

        fetch(url.toString())

});