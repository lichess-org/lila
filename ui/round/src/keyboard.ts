import RoundController from './ctrl';

export const prev = (ctrl: RoundController) => ctrl.userJump(ctrl.ply - 1);

export const next = (ctrl: RoundController) => ctrl.userJump(ctrl.ply + 1);

export const init = (ctrl: RoundController) =>
  window.Mousetrap.bind(['left', 'h'], () => {
    prev(ctrl);
    ctrl.redraw();
  })
    .bind(['right', 'l'], () => {
      next(ctrl);
      ctrl.redraw();
    })
    .bind(['up', 'k'], () => {
      ctrl.userJump(0);
      ctrl.redraw();
    })
    .bind(['down', 'j'], () => {
      ctrl.userJump(ctrl.data.steps.length - 1);
      ctrl.redraw();
    })
    .bind('f', ctrl.flipNow)
    .bind('z', () => lichess.pubsub.emit('zen'));
