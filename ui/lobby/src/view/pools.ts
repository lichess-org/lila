import { h, type Hooks } from 'snabbdom';

import { spinnerVdom, onInsert } from 'lib/view';

import type LobbyController from '../ctrl';
import * as customiser from '../customiser';

const createHandler = (ctrl: LobbyController) => (e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const id =
    (e.target as HTMLElement).dataset['id'] ||
    ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
  if (id === 'custom') {
    ctrl.isEditingPoolButtons.toggle();
    ctrl.selectedPoolButton = undefined;
  } else if (id) ctrl.clickPool(id);

  ctrl.redraw();
};

export const hooks = (ctrl: LobbyController): Hooks =>
  onInsert(el => {
    const handler = createHandler(ctrl);
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });

export function render({
  pools,
  poolMember,
  opts,
  isEditingPoolButtons,
  selectedPoolButton,
  me,
}: LobbyController) {
  const customisations = customiser.getAll(me?.username);
  const member = poolMember;
  return pools
    .map(pool => {
      const active = member?.id === pool.id;
      const transp = !!member && !active;
      const selected = isEditingPoolButtons() && selectedPoolButton === pool.id;
      const renderCustomised = customiser.renderCustomisedButton(
        pool.id,
        customisations[pool.id],
        selected,
        transp,
        isEditingPoolButtons(),
      );
      if (!active && renderCustomised) return renderCustomised;
      else
        return h(
          'div.lpool',
          {
            class: { active, transp, selected, customisable: isEditingPoolButtons() },
            attrs: { role: 'button', 'data-id': pool.id, tabindex: '0' },
          },
          [
            h('div.clock', `${pool.lim}+${pool.inc}`),
            active
              ? member.range && opts.showRatings
                ? h('div.range', member.range.replace('-', '–'))
                : spinnerVdom()
              : h('div.perf', pool.perf),
          ],
        );
    })
    .concat(
      h(
        'div.lpool',
        {
          class: { transp: !!member, selected: isEditingPoolButtons() },
          attrs: { role: 'button', 'data-id': 'custom', tabindex: '0' },
        },
        i18n.site.custom,
      ),
    );
}
