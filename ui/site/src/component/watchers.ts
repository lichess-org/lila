interface Data {
  nb: number;
  users?: string[];
  anons?: number;
  watchers?: Data;
}

let watchersData: Data | undefined;

export default function watchers(element: HTMLElement, ctrlTrans?: Trans) {
  if (element.dataset.watched) return;
  element.dataset.watched = "1";

  const listEl: HTMLElement | null = element.querySelector('.list');
  const numberEl: HTMLElement | null = element.querySelector('.number');
  lichess.pubsub.on('socket.in.crowd', data => set(data.watchers || data));

  const i18n = element.dataset.i18n;
  const trans: Trans = ctrlTrans || lichess.trans(i18n ? JSON.parse(i18n) : {});

  const set = (data: Data) => {
    watchersData = data;
    if (!data || !data.nb) return element.classList.add('none');
    if (numberEl) numberEl.textContent = trans.plural('nbSpectators', data.nb) + (listEl && data.users ? ':' : '');
    if (listEl) {
      if (data.users) {
        const anon = trans.noarg('anonymous');
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="/@/${u.includes(' ') ? u.split(' ')[1] : u}">${u}</a>` : anon
        );
        if (data.anons === 1) tags.push(anon);
        else if (data.anons) tags.push(`${anon} (${data.anons})`);
        listEl.innerHTML = tags.join(', ');
      } else if (!numberEl) listEl.innerHTML = trans.plural('nbPlayersInChat', data.nb);
      else listEl.innerHTML = '';
    }
    element.classList.remove('none');
  };

  if (watchersData) set(watchersData);
}
