import { h, type Hooks } from 'snabbdom';
import { spinnerVdom, onInsert } from 'lib/view';
import type LobbyController from '../ctrl';

const createHandler = (ctrl: LobbyController) => (e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const id =
    (e.target as HTMLElement).dataset['id'] ||
    ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
  if (id === 'custom') ctrl.setupCtrl.openModal('hook');
  else if (id) ctrl.clickPool(id);

  ctrl.redraw();
};

export const hooks = (ctrl: LobbyController): Hooks =>
  onInsert(el => {
    const handler = createHandler(ctrl);
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });

export function render({ pools, poolMember, opts }: LobbyController) {
  return pools
    .map(pool => {
      const active = poolMember?.id === pool.id;
      return h(
        'div.lpool',
        {
          class: { active, transp: !!poolMember && !active },
          attrs: { role: 'button', 'data-id': pool.id, tabindex: '0' },
        },
        [
          h('div.clock', `${pool.lim}+${pool.inc}`),
          active
            ? poolMember.range && opts.showRatings
              ? h('div.range', poolMember.range.replace('-', '–'))
              : spinnerVdom()
            : h('div.perf', pool.perf),
        ],
      );
    })
    .concat(
      h(
        'div.lpool',
        {
          class: { transp: !!poolMember },
          attrs: { role: 'button', 'data-id': 'custom', tabindex: '0' },
        },
        i18n.site.custom,
      ),
    );
}
