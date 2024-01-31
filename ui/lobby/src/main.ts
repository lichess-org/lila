import { Shogiground } from 'shogiground';
import { attributesModule, classModule, init } from 'snabbdom';
import boot from './boot';
import makeCtrl from './ctrl';
import { LobbyOpts, Tab } from './interfaces';
import view from './view/main';

export const patch = init([classModule, attributesModule]);

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
