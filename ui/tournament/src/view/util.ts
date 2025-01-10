import { initOneWithState } from 'common/mini-board';
import { numberFormat } from 'common/number';
import { dataIcon } from 'common/snabbdom';
import { type VNode, h } from 'snabbdom';
import type { Arrangement } from '../interfaces';

export function miniBoard(game: any): VNode {
  return h(
    'a.mini-board' + '.v-' + game.variant + '.mini-board-' + game.id,
    {
      key: game.id,
      attrs: {
        href: '/' + game.id + (game.color === 'sente' ? '' : '/gote'),
      },
      hook: {
        insert(vnode) {
          initOneWithState(vnode.elm as HTMLElement, {
            variant: game.variant,
            sfen: game.sfen,
            orientation: game.color,
            lastMove: game.lastMove,
          });
        },
      },
    },
    h('div.sg-wrap'),
  );
}

export function ratio2percent(r: number): string {
  return Math.round(100 * r) + '%';
}

export function playerName(p: any): VNode[] | string {
  return p.title ? [h('span.title', p.title), ' ' + p.name] : p.name;
}

export function player(
  p: any,
  asLink: boolean,
  withRating: boolean,
  defender = false,
  leader = false,
): VNode {
  return h(
    'a.ulpt.user-link' + (((p.title || '') + p.name).length > 15 ? '.long' : ''),
    {
      attrs: asLink ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
      hook: {
        destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
      },
    },
    [
      h(
        'span.name' + (defender ? '.defender' : leader ? '.leader' : ''),
        defender ? { attrs: dataIcon('5') } : leader ? { attrs: dataIcon('8') } : {},
        playerName(p),
      ),
      withRating ? h('span.rating', ' ' + p.rating + (p.provisional ? '?' : '')) : null,
    ],
  );
}

export function numberRow(name: string, value: any, typ?: string): VNode {
  return h('tr', [
    h('th', name),
    h(
      'td',
      typ === 'raw'
        ? value
        : typ === 'percent'
          ? value[1] > 0
            ? ratio2percent(value[0] / value[1])
            : 0
          : numberFormat(value),
    ),
  ]);
}

export function preloadUserTips(el: HTMLElement): void {
  window.lishogi.powertip.manualUserIn(el);
}

export function arrangementHasUser(a: Arrangement, userId: string): boolean {
  return a.user1.id === userId || a.user2.id === userId;
}

// hacky for flatpickr
export function adjustDateToUTC(date: Date): Date {
  return new Date(date.getTime() + date.getTimezoneOffset() * 60000);
}
export function adjustDateToLocal(date: Date): Date {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000);
}

export function formattedDate(date: Date, utc: boolean): string {
  if (utc) return date.toUTCString();
  else return date.toLocaleString();
}
