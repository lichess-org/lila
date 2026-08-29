import { get, set } from '@/data';
import { icons } from '@/icons';
import { pubsub } from '@/pubsub';

import { domIcon } from './makeIcon';

export interface Data {
  nb: number;
  users?: string[];
  anons?: number;
  watchers?: Data;
}

let watchersData: Data | undefined;

const name = (u: string) => (u.includes(' ') ? u.split(' ')[1] : u);

export function watchers(element: HTMLElement, withUserList = true): void {
  if (element.dataset.watched) return;
  element.dataset.watched = '1';
  const $innerElement = $('<div class="chat__members__inner">').appendTo(element);
  const $numberEl = $('<div class="chat__members__number" title="Spectators">')
    .append(domIcon(icons.User))
    .appendTo($innerElement);
  const $listEl = $('<div>').appendTo($innerElement);
  const listEl = $listEl[0] as HTMLElement;

  pubsub.on('socket.in.crowd', data => setWatchers(data.watchers || data));

  const setWatchers = (data: Data): void => {
    watchersData = data;

    if (!data?.nb) {
      element.classList.add('none');
      return;
    }

    $numberEl.text(withUserList ? String(data.nb) : i18n.broadcast.nbViewers(data.nb));

    if (data.users && withUserList) {
      const currUsers = data.users.map(u => u || '').join(';');
      const currAnons = data.anons ?? 0;
      if (get(listEl, 'prevUsers') !== currUsers || (get(listEl, 'prevAnons') ?? 0) !== currAnons) {
        set(listEl, 'prevUsers', currUsers);
        set(listEl, 'prevAnons', currAnons);
        const tags = data.users.map(u =>
          u ? `<a class="user-link ulpt" href="/@/${name(u)}">${u}</a>` : i18n.site.anonymous,
        );
        if (currAnons) tags.push(i18n.site.nbAnonymous(currAnons));
        $listEl.html(tags.join(', '));
      }
    } else $listEl.html('');

    element.classList.remove('none');
  };

  if (watchersData) setWatchers(watchersData);
}
