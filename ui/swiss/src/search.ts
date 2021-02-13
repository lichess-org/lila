import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import TournamentController from './ctrl';
import { bind } from './view/util';

export function button(ctrl: TournamentController): VNode {
  return h('button.fbt', {
    class: { active: ctrl.searching },
    attrs: {
      'data-icon': ctrl.searching ? 'L' : 'y',
      title: 'Search tournament players'
    },
    hook: bind('mousedown', ctrl.toggleSearch, ctrl.redraw)
  });
}

export function input(ctrl: TournamentController): VNode {
  return h('div.search',
    h('input', {
      hook: {
        insert(vnode) {
          requestAnimationFrame(() => {
            const el = vnode.elm as HTMLInputElement;
            window.lichess.userAutocomplete($(el), {
              tag: 'span',
              swiss: ctrl.data.id,
              focus: true,
              minLength: 3,
              onSelect(v: any) {
                ctrl.jumpToPageOf(v.id || v);
                $(el).typeahead('close');
                el.value = '';
                ctrl.redraw();
              }
            });
          });
        }
      }
    })
  );
}
