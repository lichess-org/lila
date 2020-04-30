import { Attrs } from 'snabbdom/modules/attributes'
import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { VNode } from 'snabbdom/vnode';

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el =>
    el.addEventListener(eventName, e => {
      const res = f(e);
      if (redraw) redraw();
      return res;
    })
  );
}

export function onInsert(f: (element: HTMLElement) => void): Hooks {
  return {
    insert(vnode) {
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
  return h('a.mini-board.parse-fen.is2d.mini-board-' + game.id, {
    key: game.id,
    attrs: {
      href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
      'data-color': game.color,
      'data-fen': game.fen,
      'data-lastmove': game.lastMove
    },
    hook: {
      insert(vnode) {
        window.lichess.parseFen($(vnode.elm as HTMLElement));
      }
    }
  }, [
    h('div.cg-wrap')
  ]);
}

export function playerName(p) {
  return p.title ? [h('span.title', p.title), ' ' + p.name] : p.name;
}

export function player(p: Player, asLink: boolean, withRating: boolean) {

  const fullName = playerName(p);

  return h('a.ulpt.user-link' + (fullName.length > 15 ? '.long' : ''), {
    attrs: asLink ? { href: '/@/' + p.name } : { 'data-href': '/@/' + p.name },
    hook: {
      destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement)
    }
  }, [
    h('span.name', fullName),
    withRating ? h('span.rating', ' ' + p.rating + (p.provisional ? '?' : '')) : null
  ]);
}

export function spinner(): VNode {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}
