import { EditorConfig } from "./interfaces";
import EditorCtrl from "./ctrl";
import view from "./view";

import { init } from "snabbdom";
import { VNode } from "snabbdom/vnode";
import klass from "snabbdom/modules/class";
import attributes from "snabbdom/modules/attributes";
import props from "snabbdom/modules/props";
import eventlisteners from "snabbdom/modules/eventlisteners";

import { menuHover } from "common/menuHover";
import { Shogiground } from "shogiground";

menuHover();

const patch = init([klass, attributes, props, eventlisteners]);

export default function LishogiEditor(
  element: HTMLElement,
  config: EditorConfig
) {
  let vnode: VNode, ctrl: EditorCtrl;

  const redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };

  ctrl = new EditorCtrl(config, redraw);
  element.innerHTML = "";
  const inner = document.createElement("div");
  element.appendChild(inner);
  vnode = patch(inner, view(ctrl));

  return {
    getFen: ctrl.getFen.bind(ctrl),
    setOrientation: ctrl.setOrientation.bind(ctrl),
  };
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
