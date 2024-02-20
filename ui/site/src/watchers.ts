import * as licon from 'common/licon';

interface Data {
  nb: number;
  users?: string[];
  anons?: number;
  watchers?: Data;
}

let watchersData: Data | undefined;

const name = (u: string) => (u.includes(' ') ? u.split(' ')[1] : u);

export default function watchers(element: HTMLElement) {
  if (element.dataset.watched) return;
  element.dataset.watched = '1';
  const $innerElement = $('<div class="chat__members__inner">').appendTo(element);
  const $numberEl = $(
    `<div class="chat__members__number" data-icon="${licon.User}" title="Spectators"></div>`,
  ).appendTo($innerElement);
  const $listEl = $('<div>').appendTo($innerElement);

  site.pubsub.on('socket.in.crowd', data => set(data.watchers || data));

  const set = (data: Data): void => {
    watchersData = data;

    if (!data || !data.nb) {
      element.classList.add('none');
      return;
    }

    $numberEl.text('' + data.nb);

    if (data.users) {
      const prevUsers = data.users.map(u => u || '').join(';');
      if ($listEl.data('prevUsers') !== prevUsers) {
        $listEl.data('prevUsers', prevUsers);
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="/@/${name(u)}">${u}</a>` : 'Anonymous',
        );
        if (data.anons === 1) tags.push('Anonymous');
        else if (data.anons) tags.push(`Anonymous (${data.anons})`);
        $listEl.html(tags.join(', '));
      }
    } else $listEl.html('');

    element.classList.remove('none');
  };

  if (watchersData) set(watchersData);
}
