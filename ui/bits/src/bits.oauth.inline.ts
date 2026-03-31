// ensure maximum browser compatibility here,
// as the oauth page can be embedded in very dubious webviews

const form: HTMLFormElement = document.querySelector<HTMLFormElement>('#oauth-authorize')!;
const submitBtn: HTMLButtonElement = form.querySelector<HTMLButtonElement>('button')!;

form.addEventListener('submit', function () {
  setTimeout(function () {
    document.querySelector<HTMLDivElement>('.oauth form')!.remove();
    const oauthTop = document.querySelector<HTMLDivElement>('.oauth__top')!;
    const successDiv = document.createElement('div');
    successDiv.className = 'oauth__success';
    successDiv.textContent = 'All set! You can now close this page and return to the app.';
    oauthTop.insertAdjacentElement('afterend', successDiv);
  }, 500);
});

if (submitBtn.classList.contains('auto-click') && /signup=true/.test(location.search)) {
  form.submit();
} else {
  setTimeout(
    function () {
      submitBtn.removeAttribute('disabled');
      submitBtn.classList.remove('disabled');
    },
    submitBtn.classList.contains('button-red') ? 5000 : 2000,
  );
}
