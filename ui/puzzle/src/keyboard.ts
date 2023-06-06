import * as control from './control';
import * as xhr from 'common/xhr';
import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';
import PuzzleController from './ctrl';

export const bindHotkeys = (ctrl: PuzzleController) =>
  window.Mousetrap.bind(['left', 'k'], () => {
    control.prev(ctrl);
    ctrl.redraw();
  })
    .bind(['right', 'j'], () => {
      control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], () => {
      control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], () => {
      control.last(ctrl);
      ctrl.redraw();
    })
    .bind('x', ctrl.toggleThreatMode)
    .bind('space', () => {
      if (ctrl.vm.mode === 'view') {
        if (ctrl.ceval.enabled()) ctrl.playBestMove();
        else ctrl.ceval.enable();
      }
    })
    .bind('z', () => lichess.pubsub.emit('zen'))
    .bind('?', () => ctrl.keyboardHelp(!ctrl.keyboardHelp()))
    .bind('f', ctrl.flip)
    .bind('n', ctrl.nextPuzzle);

export const view = (ctrl: PuzzleController): VNode =>
  snabModal({
    class: 'keyboard-help',
    onInsert: async ($wrap: Cash) => {
      const [, html] = await Promise.all([
        lichess.loadCssPath('puzzle.keyboard'),
        xhr.text(xhr.url('/training/help', {})),
      ]);
      $wrap.find('.scrollable').html(html);
    },
    onClose: () => ctrl.keyboardHelp(false),
    content: [h('div.scrollable', spinner())],
  });
