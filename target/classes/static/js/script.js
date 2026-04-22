const navbar = document.querySelector('.navbar');
const links = document.querySelectorAll('nav a');
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

links.forEach(link => {
  link.addEventListener('click', () => {
    navbar.classList.remove('escondida');
  });
});