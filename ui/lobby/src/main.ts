import { init } from 'snabbdom';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Shogiground } from 'shogiground';
import { LobbyOpts, Tab } from './interfaces';

export const patch = init([klass, attributes]);

import makeCtrl from './ctrl';
import view from './view/main';
import boot from './boot';

export function start(opts: LobbyOpts) {
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

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;

window.onload = function () {
  boot(window['lishogi_lobby'], document.querySelector('.lobby__app'));
};
