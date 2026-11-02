import { pubsub } from 'lib/pubsub';
import { domDialog } from 'lib/view';

import type RoundController from './ctrl';

export const prev = (ctrl: RoundController): void => ctrl.userJump(ctrl.ply - 1);

export const next = (ctrl: RoundController): void => ctrl.userJump(ctrl.ply + 1);

export const init = (ctrl: RoundController): LichessMousetrap => {
  if (location.hash === '#keyboard') showHelp();
  return site.mousetrap
    .bind(['left', 'k'], () => {
      prev(ctrl);
      ctrl.redraw();
    })
    .bind(['right', 'j'], () => {
      next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', '0', 'home'], () => {
      ctrl.userJump(0);
      ctrl.redraw();
    })
    .bind(['down', '$', 'end'], () => {
      ctrl.userJump(ctrl.data.steps.length - 1);
      ctrl.redraw();
    })
    .bind('f', ctrl.flipNow)
    .bind('z', () => pubsub.emit('zen'))
    .bind('F', ctrl.yeet)
    .bind('G', ctrl.googlyEyesStart)
    .bind('?', showHelp)
    .bind('h', ctrl.menu.toggle);
};

const showHelp = () =>
  domDialog({
    closeButton: true,
    class: 'help',
    htmlUrl: '/round/help',
    modal: true,
    show: true,
    easyClose: 'clickOutside',
  });
