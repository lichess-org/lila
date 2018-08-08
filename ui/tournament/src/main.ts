import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Draughtsground } from 'draughtsground';
import { TournamentOpts } from './interfaces';
import TournamentController from './ctrl';
import * as chat from 'chat';

const patch = init([klass, attributes]);

import makeCtrl from './ctrl';
import view from './view/main';

export function start(opts: TournamentOpts) {

  opts.classes = (opts.element.getAttribute('class') || '').replace(' ', '.');

  let vnode: VNode, ctrl: TournamentController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socket.receive
  };
};

// that's for the rest of lidraughts to access draughtsground
// without having to include it a second time
window.Draughtsground = Draughtsground;
window.LidraughtsChat = chat;
