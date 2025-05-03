import { h } from 'snabbdom';
import * as licon from 'lib/licon';
import type LobbyController from '../../../ctrl';
import { initMiniBoard } from 'lib/view/miniBoard';

export const fenInput = (ctrl: LobbyController) => {
  const { setupCtrl } = ctrl;
  const variant = setupCtrl.variant();
  if (!['fromPosition', 'chess960'].includes(variant)) return null;
  const fen = setupCtrl.fen();
  return h('div.fen.optional-config', [
    h('div.fen__form', [
      h('input#fen-input', {
        attrs: {
          placeholder:
            variant === 'chess960' ? 'Chess960 start position (FEN)' : i18n.site.pasteTheFenStringHere,
          value: fen,
        },
        on: {
          input: (e: InputEvent) => {
            setupCtrl.fen((e.target as HTMLInputElement).value.replace(/_/g, ' ').trim());
            setupCtrl.validateFen();
          },
        },
        hook: { insert: setupCtrl.validateFen },
        class: { failure: setupCtrl.fenError },
      }),
      h('a.button.button-empty', {
        attrs: {
          'data-icon': licon.Pencil,
          title: i18n.site.boardEditor,
          href:
            variant === 'chess960'
              ? `/analysis/chess960/${fen}`
              : '/editor' + (fen && !setupCtrl.fenError ? `/${fen.replace(/ /g, '_')}` : ''),
        },
      }),
    ]),
    h(
      'a.fen__board',
      { attrs: { href: `/editor/${setupCtrl.lastValidFen.replace(/ /g, '_')}` } },
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
