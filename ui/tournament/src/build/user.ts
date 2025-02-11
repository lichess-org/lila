window.lishogi.ready.then(() => {
  const button = document.querySelector('.tournamen-search button');
  button?.addEventListener('click', () => {
    const input = document.querySelector<HTMLInputElement>('.tournamen-search input');
    console.log(input?.value);
    if (input?.value)
      window.location.pathname = replaceUsername(window.location.pathname, input.value);
  });
});

function replaceUsername(url: string, newUsername: string): string {
  return url.replace(/\/(@)\/[^\/]+/, `/@/${newUsername}`);
}
