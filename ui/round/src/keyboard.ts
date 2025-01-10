import type RoundController from './ctrl';
import { lastPly } from './round';

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
};

export function prev(ctrl: RoundController): void {
  ctrl.userJump(ctrl.ply - 1);
}

export function next(ctrl: RoundController): void {
  ctrl.userJump(ctrl.ply + 1);
}

export function init(ctrl: RoundController): void {
  const k = window.lishogi.mousetrap;
  k.bind(
    ['left', 'j'],
    preventing(() => {
      prev(ctrl);
      ctrl.redraw();
    }),
  );
  k.bind(
    ['right', 'k'],
    preventing(() => {
      next(ctrl);
      ctrl.redraw();
    }),
  );
  k.bind(
    ['up', '0', 'home'],
    preventing(() => {
      ctrl.userJump(0);
      ctrl.redraw();
    }),
  );
  k.bind(
    ['down', '$', 'end'],
    preventing(() => {
      ctrl.userJump(lastPly(ctrl.data));
      ctrl.redraw();
    }),
  );
  k.bind('f', preventing(ctrl.flipNow));
  k.bind(
    'z',
    preventing(() => window.lishogi.pubsub.emit('zen')),
  );
}
