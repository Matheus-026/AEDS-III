

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

document.addEventListener('DOMContentLoaded', () => {
    // 1. Seleciona os elementos da página
    const formPesquisa = document.querySelector('.pesquisa-form');
    const btnLimpar = document.querySelector('.btn-limpar');
    const btnCancelar = document.querySelector('.btn-cancelar');
    const inputPreco = document.querySelector('input[type="range"]');
    const displayPreco = document.getElementById('valorPrecoAtual');

    // 2. Faz a bolinha do preço funcionar em tempo real
    inputPreco.addEventListener('input', function() {
        displayPreco.textContent = `R$ ${this.value}`;
    });

    // 3. Botão de Limpar os filtros
    btnLimpar.addEventListener('click', function() {
        formPesquisa.reset(); // Zera todos os inputs
        displayPreco.textContent = `R$ ${inputPreco.value}`; // Volta o texto do preço para o padrão (100)
    });

    // 4. Botão Cancelar (Apenas recarrega a página ou redireciona)
    btnCancelar.addEventListener('click', function() {
        window.location.reload(); 
    });

    // 5. Captura os dados ao clicar em BUSCAR
    formPesquisa.addEventListener('submit', function(event) {
        event.preventDefault(); // Impede a página de recarregar

        // Coleta todos os dados que têm o atributo "name"
        const formData = new FormData(formPesquisa);
        const dadosParaEnviar = Object.fromEntries(formData.entries());

        // Mostra no console do navegador o que será enviado (Aperte F12 para ver)
        console.log("JSON pronto para enviar:", dadosParaEnviar);

        // Quando sua API estiver pronta, o código de envio será parecido com este:
        /*
        fetch('http://localhost:8080/api/pesquisa', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(dadosParaEnviar)
        })
        .then(response => response.json())
        .then(data => console.log('Sucesso:', data))
        .catch(error => console.error('Erro:', error));
        */
    });
});