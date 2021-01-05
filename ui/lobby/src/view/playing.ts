import { h } from "snabbdom";
import { Shogiground } from "shogiground";
import LobbyController from "../ctrl";

function timer(pov) {
  const date = Date.now() + pov.secondsLeft * 1000;
  return h(
    "time.timeago",
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute("datetime", "" + date);
        },
      },
    },
    window.lishogi.timeago.format(date)
  );
}

export default function (ctrl: LobbyController) {
  return h(
    "div.now-playing",
    ctrl.data.nowPlaying.map(function (pov) {
      return h(
        "a." + pov.variant.key + (pov.isMyTurn ? ".my_turn" : ""),
        {
          key: pov.gameId,
          attrs: { href: "/" + pov.fullId },
        },
        [
          h("div.mini-board.cg-wrap.is2d", {
            hook: {
              insert(vnode) {
                const lm = pov.lastMove;
                Shogiground(vnode.elm as HTMLElement, {
                  coordinates: false,
                  drawable: { enabled: false, visible: false },
                  resizable: false,
                  viewOnly: true,
                  orientation: pov.color,
                  fen: pov.fen,
                  lastMove: lm && [lm[0] + lm[1], lm[2] + lm[3]],
                });
              },
            },
          }),
          h("span.meta", [
            pov.opponent.ai
              ? ctrl.trans("aiNameLevelAiLevel", "YaneuraOu", pov.opponent.ai)
              : pov.opponent.username,
            h(
              "span.indicator",
              pov.isMyTurn
                ? pov.secondsLeft && pov.hasMoved
                  ? timer(pov)
                  : [ctrl.trans.noarg("yourTurn")]
                : h("span", "\xa0")
            ), // &nbsp;
          ]),
        ]
      );
    })
  );
}
