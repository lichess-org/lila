// ensure maximum browser compatibility here,
// as the oauth page can be embedded in very dubious webviews

const el: HTMLElement = document.getElementById('oauth-authorize')!;

setTimeout(
  function () {
    el.removeAttribute('disabled');
    el.classList.remove('disabled');
  },
  el.classList.contains('button-red') ? 5000 : 2000,
);
