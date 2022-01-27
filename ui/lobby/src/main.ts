import { init, classModule, attributesModule } from 'snabbdom';
import { Chessground } from 'chessground';
import { LobbyOpts, Tab } from './interfaces';

export const patch = init([classModule, attributesModule]);

// eslint-disable-next-line no-duplicate-imports
import makeCtrl from './ctrl';
import view from './view/main';

export default function main(opts: LobbyOpts) {
  const ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  let vnode = patch(opts.element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return {
    socketReceive: ctrl.socket.receive,
    setTab(tab: Tab) {
      ctrl.setTab(tab);
      ctrl.redraw();
    },
    gameActivity: ctrl.gameActivity,
    setRedirecting: ctrl.setRedirecting,
    enterPool: ctrl.enterPool,
    leavePool: ctrl.leavePool,
    setup: ctrl.setup,
    redraw: ctrl.redraw,
  };
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
