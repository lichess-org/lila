import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';

export const fenInput = (ctrl: LobbyController) => {
  const { trans, setupCtrl } = ctrl;
  if (setupCtrl.variant() !== 'fromPosition') return null;
  return h('div.fen.optional-config', [
    h('div.fen__form', [
      h('input#fen-input', {
        attrs: { placeholder: trans('pasteTheFenStringHere'), value: setupCtrl.fen() },
        on: {
          input: (e: InputEvent) => {
            setupCtrl.fen((e.target as HTMLInputElement).value);
            setupCtrl.validateFen();
          },
        },
        hook: { insert: setupCtrl.validateFen },
        class: { failure: setupCtrl.fenError },
      }),
      h('a.button.button-empty', {
        attrs: {
          'data-icon': '',
          title: trans('boardEditor'),
          href: '/editor',
        },
      }),
    ]),
    h(
      'a.fen__board',
      { attrs: { href: '/editor' } },
      setupCtrl.fenError || !setupCtrl.lastValidFen
        ? null
        : h(
            'span.preview',
            h('div.position.mini-board.cg-wrap.is2d', {
              attrs: { 'data-state': `${setupCtrl.lastValidFen},white` },
              hook: {
                insert: vnode => lichess.miniBoard.init(vnode.elm as HTMLElement),
                update: vnode => lichess.miniBoard.init(vnode.elm as HTMLElement),
              },
            })
          )
    ),
  ]);
};
