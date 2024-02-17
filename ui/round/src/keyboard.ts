import RoundController from './ctrl';
import { lastPly } from './round';

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
};

export function prev(ctrl: RoundController) {
  ctrl.userJump(ctrl.ply - 1);
}

export function next(ctrl: RoundController) {
  ctrl.userJump(ctrl.ply + 1);
}

export function init(ctrl: RoundController) {
  const k = window.Mousetrap;
  k.bind(
    ['left', 'j'],
    preventing(function () {
      prev(ctrl);
      ctrl.redraw();
    })
  );
  k.bind(
    ['right', 'k'],
    preventing(function () {
      next(ctrl);
      ctrl.redraw();
    })
  );
  k.bind(
    ['up', '0', 'home'],
    preventing(function () {
      ctrl.userJump(0);
      ctrl.redraw();
    })
  );
  k.bind(
    ['down', '$', 'end'],
    preventing(function () {
      ctrl.userJump(lastPly(ctrl.data));
      ctrl.redraw();
    })
  );
  k.bind('f', preventing(ctrl.flipNow));
  k.bind(
    'z',
    preventing(() => window.lishogi.pubsub.emit('zen'))
  );
}
