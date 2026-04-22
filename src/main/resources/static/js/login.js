document.addEventListener("DOMContentLoaded", () => {
    
    document.getElementById('form-login').addEventListener('submit', async (e) => {
        e.preventDefault();

        const email = document.getElementById('login-email').value;
        const senha = document.getElementById('login-senha').value;

        try {
            const response = await fetch('http://localhost:8080/api/usuarios/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: email, senha: senha })
            });

            if (response.ok) {
                const usuario = await response.json();
                localStorage.setItem('usuarioLogado', JSON.stringify(usuario));
                
                if(usuario.tipo === 'Adm') {
                    window.location.href = '/adm';
                } else {
                    window.location.href = '/pesquisa';
                }
            } else {
                alert('Email ou senha incorretos!');
            }
        } catch (error) {
            console.error('Erro:', error);
            alert('Servidor offline ou erro de conexão.');
        }
    });
});