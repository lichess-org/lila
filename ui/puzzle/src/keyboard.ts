import * as control from './control';
import PuzzleCtrl from './ctrl';
import { snabDialog } from 'common/dialog';

export default (ctrl: PuzzleCtrl) =>
  lichess.mousetrap
    .bind(['left', 'k'], () => {
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
    .bind('l', ctrl.toggleCeval)
    .bind('x', ctrl.toggleThreatMode)
    .bind('space', () => {
      if (ctrl.mode === 'view') {
        if (ctrl.ceval.enabled()) ctrl.playBestMove();
        else ctrl.toggleCeval();
      }
    })
    .bind('z', () => lichess.pubsub.emit('zen'))
    .bind('?', () => ctrl.keyboardHelp(!ctrl.keyboardHelp()))
    .bind('f', ctrl.flip)
    .bind('n', ctrl.nextPuzzle)
    .bind('b', () => ctrl.blindfold(!ctrl.blindfold()));

export const view = (ctrl: PuzzleCtrl) =>
  snabDialog({
    class: 'help',
    htmlUrl: '/training/help',
    onClose: () => ctrl.keyboardHelp(false),
  });
