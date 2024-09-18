site.load.then(() => {
  document.querySelectorAll('.mod-confirm form input')?.forEach((input: HTMLInputElement) => {
    input.setSelectionRange(input.value.length, input.value.length);
    input.addEventListener('paste', () => setTimeout(() => input.form?.submit(), 50));
  });
});