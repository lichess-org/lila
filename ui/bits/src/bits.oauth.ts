// ensure maximum browser compatibility here,
// as the oauth page can be embedded in very dubious webviews

const el: HTMLElement = document.getElementById('oauth-authorize')!;
const danger: boolean = el.classList.contains('danger');

setTimeout(
  () => {
    el.removeAttribute('disabled');
    el.className = 'button';
    if (danger) el.classList.add('button-red', 'ok-cancel-confirm', 'text');
  },
  danger ? 5000 : 2000,
);
