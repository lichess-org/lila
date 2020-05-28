import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { Hooks } from 'snabbdom/hooks'
import { Attrs } from 'snabbdom/modules/attributes'

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).addEventListener(eventName, e => {
        const res = f(e);
        if (redraw) redraw();
        return res;
      });
    }
  };
}

export function onInsert(f: (element: HTMLElement) => void): Hooks {
  return {
    insert: vnode => {
      f(vnode.elm as HTMLElement)
    }
  };
}

export function dataIcon(icon: string): Attrs {
  return {
    'data-icon': icon
  };
}

export function miniBoard(game) {
  return h('a.mini-board.parse-fen.is2d.mini-board-' + game.id + '.is' + game.board.key, {
    key: game.id,
    attrs: {
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove,
      'data-board': `${game.board.size[0]}x${game.board.size[1]}`
    },
    hook: {
      insert(vnode) {
        window.lidraughts.parseFen($(vnode.elm as HTMLElement));
      }
    }
  }, [
    h('div.cg-wrap')
  ]);
}

export function ratio2percent(r: number) {
  return Math.round(100 * r) + '%';
}

export function playerName(p) {
  return p.title ? [h('span.title', p.title), ' ' + p.name] : p.name;
}

export function player(p, asLink: boolean, withRating: boolean, defender: boolean, withRatingDiff: boolean = true) {
  let ratingDiff;
  if (p.ratingDiff > 0) ratingDiff = h('span.positive', {
    attrs: { 'data-icon': 'N' }
  }, '' + p.ratingDiff);
  else if (p.ratingDiff < 0) ratingDiff = h('span.negative', {
    attrs: { 'data-icon': 'M' }
  }, '' + -p.ratingDiff);
  const rating = ' ' + p.rating + (p.provisional ? '?' : ''),
  fullName = playerName(p);

  return h('a.ulpt.user-link' + (fullName.length > 15 ? '.long' : ''), {
    attrs: asLink ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
    hook: {
      destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement)
    }
  }, [
    h('span.name' + (defender ? '.defender' : ''), defender ? { attrs: dataIcon('5') } : {}, fullName),
    withRating ? h('span.progress', withRatingDiff ? [rating, ratingDiff] : [rating]) : null
  ]);
}

export function numberRow(name: string, value: any, typ?: string) {
  return h('tr', [h('th', name), h('td',
    typ === 'raw' ? value : (typ === 'percent' ? (
      value[1] > 0 ? ratio2percent(value[0] / value[1]) : 0
    ) : window.lidraughts.numberFormat(value))
  )]);
}

export function spinner(): VNode {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
