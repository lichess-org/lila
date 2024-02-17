import { dataIcon } from 'common/snabbdom';
import { h } from 'snabbdom';

export function miniBoard(game) {
  return h(
    'a.mini-board' + '.v-' + game.variant + '.parse-sfen.mini-board-' + game.id,
    {
      key: game.id,
      attrs: {
        href: '/' + game.id + (game.color === 'sente' ? '' : '/gote'),
        'data-color': game.color,
        'data-sfen': game.sfen,
        'data-lastmove': game.lastMove,
        'data-variant': game.variant,
      },
      hook: {
        insert(vnode) {
          window.lishogi.parseSfen($(vnode.elm as HTMLElement));
        },
      },
    },
    [h('div.sg-wrap')]
  );
}

export function ratio2percent(r: number) {
  return Math.round(100 * r) + '%';
}

export function playerName(p) {
  return p.title ? [h('span.title', p.title), ' ' + p.name] : p.name;
}

export function player(p, asLink: boolean, withRating: boolean, defender: boolean = false, leader: boolean = false) {
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
        playerName(p)
      ),
      withRating ? h('span.rating', ' ' + p.rating + (p.provisional ? '?' : '')) : null,
    ]
  );
}

export function numberRow(name: string, value: any, typ?: string) {
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
          : window.lishogi.numberFormat(value)
    ),
  ]);
}
