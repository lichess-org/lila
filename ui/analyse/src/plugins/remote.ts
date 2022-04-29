import { RemoteWorkerOpts } from 'ceval';

lichess.load
  .then(() => new Promise(resolve => setTimeout(resolve, 2000)))
  .then(() => {
    const btn = document.getElementById('engine-authorize')!;
    btn.addEventListener('click', () => {
      const url = new URL(document.location.href);
      const opts: RemoteWorkerOpts = {
        url: url.searchParams.get('url')!,
        name: url.searchParams.get('name')!,
        maxThreads: parseInt(url.searchParams.get('maxThreads') || '1', 10),
      };
      lichess.storage.set('ceval.remote', JSON.stringify(opts));
      document.location.href = '/analysis';
    });
    btn.removeAttribute('disabled');
    btn.setAttribute('class', 'button');
  });
