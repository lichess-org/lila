import * as control from './control';
import { KeyboardController } from './interfaces';

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
};

export default function (ctrl: KeyboardController): void {
  window.Mousetrap.bind(['left', 'k'], () => {
    control.prev(ctrl);
    ctrl.redraw();
  })
    .bind(
      ['right', 'j'],
      preventing(() => {
        control.next(ctrl);
        ctrl.redraw();
      })
    )
    .bind(
      ['up', '0'],
      preventing(() => {
        control.first(ctrl);
        ctrl.redraw();
      })
    )
    .bind(
      ['down', '$'],
      preventing(() => {
        control.last(ctrl);
        ctrl.redraw();
      })
    )
    .bind(
      'l',
      preventing(() => {
        ctrl.toggleCeval;
      })
    )
    .bind(
      'x',
      preventing(() => {
        ctrl.toggleThreatMode;
      })
    )
    .bind(
      'space',
      preventing(() => {
        if (ctrl.vm.mode === 'view') {
          if (ctrl.getCeval().enabled()) ctrl.playBestMove();
          else ctrl.toggleCeval();
        }
      })
    )
    .bind(
      'z',
      preventing(() => {
        window.lishogi.pubsub.emit('zen');
      })
    );
}
