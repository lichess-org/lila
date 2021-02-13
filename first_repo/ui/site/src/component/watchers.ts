interface Data {
  nb: number;
  users?: string[];
  anons?: number;
  watchers?: Data;
}

let watchersData: Data | undefined;

export default function watchers(element: HTMLElement) {
  const listEl: HTMLElement | null = element.querySelector('.list');
  const numberEl: HTMLElement | null = element.querySelector('.number');
  lichess.pubsub.on('socket.in.crowd', data => set(data.watchers || data));

  const set = (data: Data) => {
    watchersData = data;
    if (!data || !data.nb) return element.classList.add('none');
    if (numberEl) numberEl.textContent = '' + data.nb;
    if (data.users && listEl) {
      const tags = data.users.map(u =>
        u ? `<a class="user-link ulpt" href="/@/${u.includes(' ') ? u.split(' ')[1] : u}">${u}</a>` : 'Anonymous'
      );
      if (data.anons === 1) tags.push('Anonymous');
      else if (data.anons) tags.push('Anonymous (' + data.anons + ')');
      listEl.innerHTML = tags.join(', ');
    } else if (!numberEl && listEl) listEl.innerHTML = `${data.nb} players in the chat`;
    element.classList.remove('none');
  };

  if (watchersData) set(watchersData);
}
