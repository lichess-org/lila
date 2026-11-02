import { get, set } from '@/data';
import { licon } from '@/licon';
import { pubsub } from '@/pubsub';

import { profileUrl } from './userLink';

export interface Data {
  nb: number;
  users?: string[];
  watchers?: Data;
}

let watchersData: Data | undefined;

const name = (u: string) => (u.includes(' ') ? u.split(' ')[1] : u);

export function watchers(element: HTMLElement, withUserList = true): void {
  if (element.dataset.watched) return;
  element.dataset.watched = '1';
  const $innerElement = $('<div class="chat__members__inner">').appendTo(element);
  const $numberEl = $(
    `<div class="chat__members__number" data-icon="${licon.User}" title="Spectators"></div>`,
  ).appendTo($innerElement);
  const $listEl = $('<div>').appendTo($innerElement);
  const listEl = $listEl[0] as HTMLElement;

  pubsub.on('socket.in.crowd', data => setWatchers(data.watchers || data));

  const setWatchers = (data: Data): void => {
    watchersData = data;

    if (!data.nb && data.users) data.nb = data.users.length;

    if (!data.nb) {
      element.classList.add('none');
      return;
    }

    $numberEl.text(withUserList ? String(data.nb) : i18n.broadcast.nbViewers(data.nb));

    if (data.users && withUserList) {
      const currUsers = data.users.map(u => u || '').join(';');
      if (get(listEl, 'prevUsers') !== currUsers) {
        set(listEl, 'prevUsers', currUsers);
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="${profileUrl(name(u))}">${u}</a>` : i18n.site.anonymous,
        );
        $listEl.html(tags.join(', '));
      }
    } else $listEl.html('');

    element.classList.remove('none');
  };

  if (watchersData) setWatchers(watchersData);
}
