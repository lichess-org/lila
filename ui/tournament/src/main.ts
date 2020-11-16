import { init } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import klass from "snabbdom/modules/class";
import attributes from "snabbdom/modules/attributes";
import { Shogiground } from "shogiground";
import { TournamentOpts } from "./interfaces";
import TournamentController from "./ctrl";
import LishogiChat from "chat";

const patch = init([klass, attributes]);

import makeCtrl from "./ctrl";
import view from "./view/main";

export function start(opts: TournamentOpts) {
  opts.classes = opts.element.getAttribute("class");
  opts.$side = $(".tour__side").clone();
  opts.$faq = $(".tour__faq").clone();

  let vnode: VNode, ctrl: TournamentController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = "";
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socket.receive,
  };
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
window.LishogiChat = LishogiChat;
