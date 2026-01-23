import { h } from 'snabbdom';
import * as licon from 'lib/licon';
import { initMiniBoard } from 'lib/view';
import type SetupController from '@/setupCtrl';

export const fenInput = (ctrl: SetupController) => {
  if (ctrl.variant() !== 'fromPosition') return null;
  const fen = ctrl.fen();
  return h('div.config-group', [
    h('div.fen__form', [
      h('input#fen-input', {
        attrs: { placeholder: i18n.site.pasteTheFenStringHere, value: fen },
        on: {
          input: (e: InputEvent) => {
            ctrl.fen((e.target as HTMLInputElement).value.replace(/_/g, ' ').trim());
            ctrl.validateFen();
          },
        },
        hook: { insert: ctrl.validateFen },
        class: { failure: ctrl.fenError },
      }),
      h('a.button.button-empty', {
        attrs: {
          'data-icon': licon.Pencil,
          title: i18n.site.boardEditor,
          href: '/editor' + (fen && !ctrl.fenError ? `/${fen.replace(/ /g, '_')}` : ''),
        },
      }),
    ]),
    h(
      'a.fen__board',
      { attrs: { href: `/editor/${ctrl.lastValidFen.replace(/ /g, '_')}` } },
      !ctrl.lastValidFen || !ctrl.validFen()
        ? null
        : h('div.position.mini-board.cg-wrap.is2d', {
            attrs: { 'data-state': `${ctrl.lastValidFen},white` },
            hook: {
              insert: vnode => initMiniBoard(vnode.elm as HTMLElement),
              update: vnode => initMiniBoard(vnode.elm as HTMLElement),
            },
          }),
    ),
  ]);
};
