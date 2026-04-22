document.addEventListener("DOMContentLoaded", () => {
    
    document.getElementById('form-cadastro').addEventListener('submit', async (e) => {
        e.preventDefault();

        const nome = document.getElementById('cad-nome').value;
        const email = document.getElementById('cad-email').value;
        const senha = document.getElementById('cad-senha').value;
        const confirmaSenha = document.getElementById('cad-senha-confirma').value;

        if (senha !== confirmaSenha) {
            alert("As senhas não coincidem!");
            return;
        }

        const novoUsuario = {
            nome: nome,
            email: email,
            senha: senha,
            tipo: "Standard"
        };

        try {
            const response = await fetch('http://localhost:8080/api/usuarios', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(novoUsuario)
            });

            if (response.ok) {
                alert('Cadastro realizado com sucesso! Faça login.');
                window.location.href = '/login';
            } else {
                alert('Erro ao realizar o cadastro.');
            }
        } catch (error) {
            console.error('Erro:', error);
            alert('Servidor offline ou erro de conexão.');
        }
    });
});