import { init } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import klass from "snabbdom/modules/class";
import attributes from "snabbdom/modules/attributes";
import { Shogiground } from "shogiground";
import { SwissOpts } from "./interfaces";
import SwissCtrl from "./ctrl";
import LishogiChat from "chat";

const patch = init([klass, attributes]);

import view from "./view/main";

export function start(opts: SwissOpts) {
  const li = window.lishogi;
  const element = document.querySelector("main.swiss") as HTMLElement;
  li.socket = li.StrongSocket(
    "/swiss/" + opts.data.id,
    opts.data.socketVersion,
    {
      receive: (t: string, d: any) => ctrl.socket.receive(t, d),
    }
  );
  opts.classes = element.getAttribute("class");
  opts.socketSend = li.socket.send;
  opts.element = element;
  opts.$side = $(".swiss__side").clone();

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new SwissCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = "";
  vnode = patch(element, blueprint);

  redraw();
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
window.LishogiChat = LishogiChat;
