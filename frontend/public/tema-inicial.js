// Fija el tema antes del primer pintado para evitar un destello del tema equivocado.
// Va en un archivo aparte porque la política de seguridad de Nginx no permite scripts en línea.
(function () {
  var guardado = null;
  try {
    guardado = localStorage.getItem('adminpro.tema');
  } catch (e) {
    guardado = null;
  }
  var oscuro = guardado ? guardado === 'dark' : matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.setAttribute('data-theme', oscuro ? 'dark' : 'light');
})();
