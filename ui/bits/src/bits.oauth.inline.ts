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

el.addEventListener('click', function () {
  document.querySelector<HTMLDivElement>('.oauth form')!.remove();
  const oauthTop = document.querySelector<HTMLDivElement>('.oauth__top')!;
  const successDiv = document.createElement('div');
  successDiv.className = 'oauth__success';
  successDiv.textContent = 'All set! You can now close this page and return to the app.';
  oauthTop.insertAdjacentElement('afterend', successDiv);
});
