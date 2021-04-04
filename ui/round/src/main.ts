import { attributesModule } from 'snabbdom';
import boot from './boot';
import { classModule } from 'snabbdom';
import LichessChat from 'chat';
import menuHover from 'common/menuHover';
import MoveOn from './moveOn';
import RoundController from './ctrl';
import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { main as view } from './view/main';
import { RoundOpts } from './interfaces';
import { VNode } from 'snabbdom';

export interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}

export interface RoundMain {
  app: (opts: RoundOpts) => RoundApi;
}

const patch = init([classModule, attributesModule]);

export function app(opts: RoundOpts): RoundApi {
  let vnode: VNode, ctrl: RoundController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new RoundController(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  window.addEventListener('resize', redraw); // col1 / col2+ transition

  if (ctrl.isPlaying()) menuHover();

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn,
  };
}

export { boot };

window.LichessChat = LichessChat;
// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
