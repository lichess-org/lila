import { spinnerVdom, onInsert, div, makeExoticTag } from 'lib/view';

import type LobbyController from '../ctrl';

const createHandler = (ctrl: LobbyController) => (e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const target = e.target as HTMLElement;
  const id = target.dataset['id'] || (target.parentNode as HTMLElement).dataset['id'];
  if (id === 'custom') ctrl.setupCtrl.openModal('hook');
  else if (id) ctrl.clickPool(id);

  ctrl.redraw();
};

export const hooks = (ctrl: LobbyController) =>
  onInsert(el => {
    const handler = createHandler(ctrl);
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });

const poolButton = makeExoticTag('div.lpool', {
  role: 'button',
  tabindex: '0',
});

export function render({ pools, poolMember, opts }: LobbyController) {
  return pools
    .map(pool => {
      const active = poolMember?.id === pool.id;
      return poolButton(
        {
          class: { active, transp: !!poolMember && !active },
          'data-id': pool.id,
        },
        [
          div('.clock', `${pool.lim}+${pool.inc}`),
          active
            ? poolMember.range && opts.showRatings
              ? div('.range', poolMember.range.replace('-', '–'))
              : spinnerVdom()
            : div('.perf', pool.perf),
        ],
      );
    })
    .concat(
      poolButton(
        {
          class: { transp: !!poolMember },
          'data-id': 'custom',
        },
        i18n.site.custom,
      ),
    );
}
