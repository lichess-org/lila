import * as control from './control';
import type PuzzleCtrl from './ctrl';
import { snabDialog } from 'lib/view/dialog';
import { pubsub } from 'lib/pubsub';

export default (ctrl: PuzzleCtrl) =>
  site.mousetrap
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
    .bind('l', () => ctrl.cevalEnabled(!ctrl.cevalEnabled()))
    .bind('x', ctrl.toggleThreatMode)
    .bind('space', () => {
      if (ctrl.mode === 'view') {
        if (ctrl.cevalEnabled()) ctrl.playBestMove();
        else ctrl.cevalEnabled(true);
      }
    })
    .bind('z', () => pubsub.emit('zen'))
    .bind('?', () => ctrl.keyboardHelp(!ctrl.keyboardHelp()))
    .bind('f', ctrl.flip)
    .bind('n', ctrl.nextPuzzle);

export const view = (ctrl: PuzzleCtrl) =>
  snabDialog({
    class: 'help',
    htmlUrl: '/training/help',
    onClose: () => ctrl.keyboardHelp(false),
    modal: true,
  });
