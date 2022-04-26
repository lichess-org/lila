import * as control from './control';
import { KeyboardController } from './interfaces';
import { h, VNode } from 'snabbdom';
import * as xhr from 'common/xhr';
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
    .bind('?', () => {
      ctrl.keyboardHelp = !ctrl.keyboardHelp;
      ctrl.redraw();
    })
    .bind('f', ctrl.flip)
    .bind('n', () => {
      if (ctrl.vm.mode === 'view') ctrl.nextPuzzle();
    });

export function view(ctrl: RoundController): VNode {
  return snabModal({
    class: 'keyboard-help',
    onInsert: async ($wrap: Cash) => {
      const [, html] = await Promise.all([lichess.loadCssPath('round.keyboard'), xhr.text(xhr.url('/round/help', {}))]);
      $wrap.find('.scrollable').html(html);
    },
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
    content: [h('div.scrollable', spinner())],
  });
}
