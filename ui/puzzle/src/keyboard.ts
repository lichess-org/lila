import { pubsub } from 'lib/pubsub';
import { domDialog } from 'lib/view';

import * as control from './control';
import type PuzzleCtrl from './ctrl';

export default (ctrl: PuzzleCtrl) => {
  if (location.hash === '#keyboard') showHelp();
  return site.mousetrap
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
    .bind('l', () => {
      if (ctrl.isCevalAllowed()) ctrl.cevalEnabled(!ctrl.cevalEnabled());
    })
    .bind('x', ctrl.toggleThreatMode)
    .bind('space', () => {
      if (ctrl.isCevalAllowed()) {
        if (ctrl.cevalEnabled()) ctrl.playBestMove();
        else ctrl.cevalEnabled(true);
      }
    })
    .bind('z', () => pubsub.emit('zen'))
    .bind('?', showHelp)
    .bind('f', ctrl.flip)
    .bind('n', ctrl.nextPuzzle)
    .bind('h', ctrl.menu.toggle)
    .bind('G', ctrl.googlyEyesStart);
};

const showHelp = () =>
  domDialog({
    class: 'help',
    htmlUrl: '/training/help',
    show: true,
    modal: true,
    easyClose: 'clickOutside',
  });
