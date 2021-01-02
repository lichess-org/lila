import { h } from "snabbdom";
import { VNode } from "snabbdom/vnode";

import { Redraw, Close, bind, header } from "./util";

type Key = string;

export type Notation = string[];

export interface NotationData {
  current: Key;
  list: Notation;
}

export interface NotationCtrl {
  makeList(): Notation[];
  set(k: Key): void;
  data: () => NotationData;
  redraw: Redraw;
  trans: Trans;
  close: Close;
}

export function ctrl(
  data: NotationData,
  trans: Trans,
  redraw: Redraw,
  close: Close
): NotationCtrl {

  const list: Notation[] = data.list.map((n) => n.split(" "));
  function getData() {
    return data;
  }

  return {
    makeList() {
      return list;
    },
    set(k: Key) {
        data.current = k;
        $.post("/pref/pieceNotation", { pieceNotation: k }).fail(() =>
          window.lishogi.announce({ msg: "Failed to save notation preference" })
        );
      redraw();
      // we need to reload the page to see changes - kinda stupid solution, but it works
      setTimeout(location.reload.bind(location), 250);
    },
    data: getData,
    redraw,
    trans,
    close,
  };
}

export function view(ctrl: NotationCtrl): VNode {

  return h(
    "div.sub.sound.", // too lazy to add notation specific css
    {
    },
    [
      header(ctrl.trans("notationSystem"), ctrl.close),
      h("div.content", [
        h("div.selector", ctrl.makeList().map(notationView(ctrl, ctrl.data().current.toString()))),
      ]),
    ]
  );
}

function notationView(ctrl: NotationCtrl, current: Key) {
  return (s: Notation) =>
    h(
      "a.text",
      {
        hook: bind("click", () => ctrl.set(s[0])),
        class: { active: current === s[0] },
        attrs: { "data-icon": "E" },
      },
      notationDisplay(s[1])
    );
}

function notationDisplay(notation: string): string {
  switch (notation) {
    case "Western": return  "Western        - P-76";
    case "Western2": return "Western        - P-7f";
    case "Japanese": return "Japanese       - ７六歩";
    case "Kawasaki": return "Kitao-Kawasaki - 歩-76";
    default: return notation;
  } 
}
