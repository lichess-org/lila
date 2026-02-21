import { h } from 'snabbdom';
import { spinnerVdom } from 'lib/view';
import type LobbyController from '../ctrl';

export const handler = (ctrl: LobbyController, e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const id =
    (e.target as HTMLElement).dataset['id'] ||
    ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
  if (id === 'custom') ctrl.setPoolMode('quick_pairing');
  else if (id) ctrl.clickPool(id);
};

export function render(ctrl: LobbyController) {
  const member = ctrl.poolMember;
  return ctrl.pools
    .map(pool => {
      const active = member?.id === pool.id,
        transp = !!member && !active;
      return h(
        'div.lpool',
        {
          class: { active, transp },
          attrs: { role: 'button', 'data-id': pool.id, tabindex: '0' },
        },
        [
          h('div.clock', `${pool.lim}+${pool.inc}`),
          active
            ? member.range && ctrl.opts.showRatings
              ? h('div.range', member.range.replace('-', '–'))
              : spinnerVdom()
            : h('div.perf', 'custom'),
        ],
      );
    })
    .concat(
      h(
        'div.lpool',
        {
          class: { transp: !!member },
          attrs: { role: 'button', 'data-id': 'custom', tabindex: '0' },
        },
        i18n.site.quickPairing,
      ),
    );
}
