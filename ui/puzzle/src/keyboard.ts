import * as control from './control';
import { KeyboardController } from './interfaces';

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
    .bind('f', ctrl.flip)
    .bind('n', () => {
      if (ctrl.vm.mode === 'view') ctrl.nextPuzzle();
    });
