import { h } from 'snabbdom';
import * as licon from 'common/licon';
import { SetupCtrl } from '../../ctrl';

export const fenInput = (ctrl: SetupCtrl) => {
  if (ctrl.variant() !== 'fromPosition') return null;
  const fen = ctrl.fen();
  return h('div.fen.optional-config', [
    h('div.fen__form', [
      h('input#fen-input', {
        attrs: { placeholder: ctrl.root.trans('pasteTheFenStringHere'), value: fen },
        on: {
          input: (e: InputEvent) => {
            ctrl.fen((e.target as HTMLInputElement).value.trim());
            ctrl.validateFen();
          },
        },
        hook: { insert: ctrl.validateFen },
        class: { failure: ctrl.fenError },
      }),
      h('a.button.button-empty', {
        attrs: {
          'data-icon': licon.Pencil,
          title: ctrl.root.trans('boardEditor'),
          href: '/editor' + (fen && !ctrl.fenError ? `/${fen.replace(' ', '_')}` : ''),
        },
      }),
    ]),
    h(
      'a.fen__board',
      { attrs: { href: `/editor/${ctrl.lastValidFen.replace(' ', '_')}` } },
      !ctrl.lastValidFen || !ctrl.validFen()
        ? null
        : h(
            'span.preview',
            h('div.position.mini-board.cg-wrap.is2d', {
              attrs: { 'data-state': `${ctrl.lastValidFen},white` },
              hook: {
                insert: vnode => lichess.miniBoard.init(vnode.elm as HTMLElement),
                update: vnode => lichess.miniBoard.init(vnode.elm as HTMLElement),
              },
            }),
          ),
    ),
  ]);
};
