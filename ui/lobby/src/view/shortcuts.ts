import { type Hooks } from 'snabbdom';

import { myUserId } from 'lib';
import { licon } from 'lib/licon';
import type { LobbyShortcut } from 'lib/types';
import { div, onInsert, spinnerVdom, hl } from 'lib/view';

import type LobbyController from '../ctrl';
import { pools, fitShortcut } from '../shortcutsCtrl';

const createHandler = (ctrl: LobbyController) => (e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const id =
    (e.target as HTMLElement).dataset['id'] ||
    ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
  if (id) ctrl.shortcutsCtrl.onclick(id);

  ctrl.redraw();
};

export const hooks = (ctrl: LobbyController): Hooks =>
  onInsert(el => {
    const handler = createHandler(ctrl);
    el.addEventListener('click', handler);
    el.addEventListener('keydown', handler);
  });

export function render(ctrl: LobbyController) {
  const { shortcutsCtrl, poolMember, opts } = ctrl;
  return [
    ...shortcutsCtrl.configured.map((shortcut: LobbyShortcut | null) => {
      if (!shortcut) return div({ 'aria-hidden': true });

      const active = poolMember?.id === shortcut.id;
      const pool = pools.find(p => p.id === shortcut.id);
      if (!pool) return renderShortcut(shortcut, Boolean(poolMember) && !active);
      return div(
        {
          class: { shortcut: true, active, transp: !!poolMember && !active },
          role: 'button',
          'data-id': pool.id,
          tabindex: 0,
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
    }),
    myUserId()
      ? renderShortcut(shortcutsCtrl.get('customize')!, Boolean(poolMember))
      : div(
          '.shortcut',
          { role: 'button', tabindex: 0, on: { click: () => ctrl.setupCtrl.openModal('hook') } },
          i18n.site.custom,
        ),
  ];
}

function renderShortcut(s: LobbyShortcut, dimmed: boolean) {
  const { scale: em, text } = fitShortcut(s);
  return hl(
    'div',
    {
      class: { shortcut: true, transp: dimmed },
      attrs: { role: 'button', 'data-id': s.id, tabindex: 0, style: `---scale: ${em}` },
    },
    [
      s.iconUrl && div('.icon', hl('img', { attrs: { src: s.iconUrl, alt: '' } })),
      s.iconMaskUrl &&
        div('.icon', div('.mask', { attrs: { class: 'mask', style: `---icon-mask:url(${s.iconMaskUrl})` } })),
      s.iconKey && div('.icon', hl('i', { attrs: { 'data-icon': licon[s.iconKey] } })),
      div('.name', { style: { fontSize: `${em}em` } }, text),
    ],
  );
}
