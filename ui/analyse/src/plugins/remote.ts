lichess.load.then(() => new Promise(resolve => setTimeout(resolve, 2000))).then(() => {
  const btn = document.getElementById('engine-authorize')!;
  btn.addEventListener('click', () => {
    const url = new URL(document.location.href);
    const threads = url.searchParams.get('threads');
    const hash = url.searchParams.get('hash');
    lichess.storage.set('ceval.remote', JSON.stringify({
      url: url.searchParams.get('url'),
      threads: threads ? parseInt(threads, 10) : undefined,
      hash: hash ? parseInt(hash, 10) : undefined,
    }));
    document.location.href = '/analysis';
  });
  btn.removeAttribute('disabled');
  btn.setAttribute('class', 'button');
});
