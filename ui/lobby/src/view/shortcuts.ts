import { type Hooks } from 'snabbdom';

import { myUserId } from 'lib';
import { licon } from 'lib/licon';
import type { LobbyShortcut } from 'lib/types';
import { div, onInsert, spinnerVdom, hl, button } from 'lib/view';

import type LobbyController from '../ctrl';
import { fitShortcut } from '../shortcutsCtrl';

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
  const editShortcuts = button('.edit-shortcuts', {
    attrs: { 'data-icon': licon.Star },
    on: {
      click: () =>
        site.asset
          .loadEsm('lobby.shortcutsDialog', { init: { ctrl: ctrl.shortcutsCtrl } })
          .then(() => ctrl.redraw()),
    },
  });
  const shortcuts = shortcutsCtrl.configured.map((shortcut: LobbyShortcut | null) => {
    if (!shortcut) return div({ 'aria-hidden': true });
    const active = poolMember?.id === shortcut.id;
    return !shortcut.pool
      ? renderShortcut(shortcut, Boolean(poolMember) && !active)
      : div(
          {
            class: { shortcut: true, active, transp: !!poolMember && !active },
            role: 'button',
            'data-id': shortcut.id,
            tabindex: 0,
          },
          [
            hl('div.clock', shortcut.pool),
            active
              ? poolMember.range && opts.showRatings
                ? div('.range', poolMember.range.replace('-', '–'))
                : spinnerVdom()
              : div('.perf', shortcut.name),
          ],
        );
  });
  if (myUserId()) shortcuts.splice(3, 0, editShortcuts); // it's absolute positioned, this is for tab order
  return shortcuts;
}

function renderShortcut(s: LobbyShortcut, dimmed: boolean) {
  const { scale, text } = fitShortcut(s);
  return hl(
    'div',
    {
      class: { shortcut: true, transp: dimmed },
      attrs: { role: 'button', 'data-id': s.id, tabindex: 0, style: `---scale: ${scale}` },
    },
    [
      s.iconUrl && div('.icon', hl('img', { attrs: { src: s.iconUrl, alt: '' } })),
      s.iconMaskUrl &&
        div('.icon', div('.mask', { attrs: { class: 'mask', style: `---icon-mask:url(${s.iconMaskUrl})` } })),
      s.iconKey && div('.icon', hl('i', { attrs: { 'data-icon': licon[s.iconKey] } })),
      div('.name', { style: { fontSize: `${scale}em` } }, text),
    ],
  );
}
