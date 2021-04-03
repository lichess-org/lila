import { init } from 'snabbdom';
import { VNode } from 'snabbdom';
import { classModule } from 'snabbdom';
import { attributesModule } from 'snabbdom';
import { Chessground } from 'chessground';
import { LobbyOpts, Tab } from './interfaces';
import LobbyController from './ctrl';

export const patch = init([classModule, attributesModule]);

import makeCtrl from './ctrl';
import view from './view/main';

export default function main(opts: LobbyOpts) {
  let vnode: VNode, ctrl: LobbyController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

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
