import * as control from './control';
import * as xhr from 'common/xhr';
import { Controller, KeyboardController } from './interfaces';
import { h, VNode } from 'snabbdom';
import { snabModal } from 'common/modal';
import { spinnerVdom as spinner } from 'common/spinner';

export default (ctrl: KeyboardController) =>
  window.Mousetrap.bind(['left', 'k'], () => {
    control.prev(ctrl);
    ctrl.redraw();
  })
    .bind(['right', 'j'], () => {
      control.next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0'], () => {
      control.first(ctrl);
      ctrl.redraw();
    })
    .bind(['down', '$'], () => {
      control.last(ctrl);
      ctrl.redraw();
    })
    .bind('l', ctrl.toggleCeval)
    .bind('x', ctrl.toggleThreatMode)
    .bind('space', () => {
      if (ctrl.vm.mode === 'view') {
        if (ctrl.getCeval().enabled()) ctrl.playBestMove();
        else ctrl.toggleCeval();
      }
    })
    .bind('z', () => lichess.pubsub.emit('zen'))
    .bind('?', () => ctrl.keyboardHelp(!ctrl.keyboardHelp()))
    .bind('f', ctrl.flip)
    .bind('n', () => {
      if (ctrl.vm.mode === 'view') ctrl.nextPuzzle();
    });

export const view = (ctrl: Controller): VNode =>
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
