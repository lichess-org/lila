import { Shogiground } from "shogiground";
import { init } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import klass from "snabbdom/modules/class";
import attributes from "snabbdom/modules/attributes";

import { RoundOpts } from "./interfaces";
import RoundController from "./ctrl";
import MoveOn from "./moveOn";
import { main as view } from "./view/main";
import LishogiChat from "chat";
import boot from "./boot";
import { menuHover } from "common/menuHover";

export interface RoundApi {
  socketReceive(typ: string, data: any): boolean;
  moveOn: MoveOn;
}

export interface RoundMain {
  app: (opts: RoundOpts) => RoundApi;
}

export function app(opts: RoundOpts): RoundApi {
  const patch = init([klass, attributes]);

  let vnode: VNode, ctrl: RoundController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new RoundController(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = "";
  vnode = patch(opts.element, blueprint);

  window.addEventListener("resize", redraw); // col1 / col2+ transition

  ctrl.isPlaying() && menuHover();

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn,
  };
}

export { boot };

window.LishogiChat = LishogiChat;
// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
