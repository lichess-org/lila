import { h, VNode } from 'snabbdom';
import { bind, onInsert } from 'common/snabbdom';
import TournamentController from './ctrl';

export function button(ctrl: TournamentController): VNode {
  return h('button.fbt', {
    class: { active: ctrl.searching },
    attrs: {
      'data-icon': ctrl.searching ? '' : '',
      title: 'Search tournament players',
    },
    hook: bind('click', ctrl.toggleSearch, ctrl.redraw),
  });
}

export function input(ctrl: TournamentController): VNode {
  return h(
    'div.search',
    h('input', {
      hook: onInsert((el: HTMLInputElement) => {
        lichess.userComplete().then(uac => {
          uac({
            input: el,
            tour: ctrl.data.id,
            tag: 'span',
            focus: true,
            onSelect(v) {
              ctrl.jumpToPageOf(v.id);
              ctrl.redraw();
            },
          });
          el.focus();
        });
        $(el).on('keydown', e => {
          if (e.code === 'Enter') {
            const rank = parseInt(e.target.value.replace('#', '').trim());
            if (rank > 0) ctrl.jumpToRank(rank);
          }
          if (e.code === 'Escape') {
            ctrl.toggleSearch();
            ctrl.redraw();
          }
        });
      }),
    })
  );
}
