import RoundController from './ctrl';

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
}

export function prev(ctrl: RoundController) {
  ctrl.userJump(ctrl.ply - 1);
}

export function next(ctrl: RoundController) {
  ctrl.userJump(ctrl.ply + 1);
}

export function init(ctrl: RoundController) {
  const k = window.Mousetrap;
  k.bind(['left', 'h'], preventing(function() {
    prev(ctrl);
    ctrl.redraw();
  }));
  k.bind(['right', 'l'], preventing(function() {
    next(ctrl);
    ctrl.redraw();
  }));
  k.bind(['up', 'k'], preventing(function() {
    ctrl.userJump(0);
    ctrl.redraw();
  }));
  k.bind(['down', 'j'], preventing(function() {
    ctrl.userJump(ctrl.data.steps.length - 1);
    ctrl.redraw();
  }));
  k.bind('f', preventing(ctrl.flipNow));
  k.bind('z', preventing(() => window.lichess.pubsub.emit('zen')));
}
