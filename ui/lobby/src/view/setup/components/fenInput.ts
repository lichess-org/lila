import { h } from 'snabbdom';
import * as licon from 'common/licon';
import LobbyController from '../../../ctrl';
import { initMiniBoard } from 'common/miniBoard';

export const fenInput = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  if (setupCtrl.variant() !== 'fromPosition') return null;
  const fen = setupCtrl.fen();
  return h('div.fen.optional-config', [
    h('div.fen__form', [
      h('input#fen-input', {
        attrs: { placeholder: i18n.site.pasteTheFenStringHere(), value: fen },
        on: {
          input: (e: InputEvent) => {
            setupCtrl.fen((e.target as HTMLInputElement).value.trim());
            setupCtrl.validateFen();
          },
        },
        hook: { insert: setupCtrl.validateFen },
        class: { failure: setupCtrl.fenError },
      }),
      h('a.button.button-empty', {
        attrs: {
          'data-icon': licon.Pencil,
          title: i18n.site.boardEditor(),
          href: '/editor' + (fen && !setupCtrl.fenError ? `/${fen.replace(' ', '_')}` : ''),
        },
      }),
    ]),
    h(
      'a.fen__board',
      { attrs: { href: `/editor/${setupCtrl.lastValidFen.replace(' ', '_')}` } },
      !setupCtrl.lastValidFen || !setupCtrl.validFen()
        ? null
        : h(
            'span.preview',
            h('div.position.mini-board.cg-wrap.is2d', {
              attrs: { 'data-state': `${setupCtrl.lastValidFen},white` },
              hook: {
                insert: vnode => initMiniBoard(vnode.elm as HTMLElement),
                update: vnode => initMiniBoard(vnode.elm as HTMLElement),
              },
            }),
          ),
    ),
  ]);
};
