import {
  parseUci,
  makeSquare,
  shogiToChessUci,
  promotesTo,
} from "shogiutil/util";
import { isDrop } from "shogiutil/types";

import { winningChances } from "ceval";
import * as cg from "shogiground/types";
import { opposite } from "shogiground/util";
import { DrawShape } from "shogiground/draw";
import AnalyseCtrl from "./ctrl";

function pieceDrop(key: cg.Key, role: cg.Role, color: Color): DrawShape {
  return {
    orig: key,
    piece: {
      color,
      role,
      scale: 0.8,
    },
    brush: "green",
  };
}

export function makeShapesFromUci(
  color: Color,
  uci: Uci,
  brush: string,
  pieces?: cg.Pieces,
  modifiers?: any
): DrawShape[] {
  uci = shogiToChessUci(uci);
  const move = parseUci(uci)!;
  const to = makeSquare(move.to);
  if (isDrop(move))
    return [{ orig: to, brush }, pieceDrop(to, move.role, color)];

  const shapes: DrawShape[] = [
    {
      orig: makeSquare(move.from),
      dest: to,
      brush,
      modifiers,
    },
  ];
  if (move.promotion && pieces)
    shapes.push(pieceDrop(to, promotesTo((pieces.get(uci.slice(0, 2) as Key)!.role)), color));
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = ctrl.node.fen.includes(" w ") ? "white" : "black"; //todo
  const rcolor = opposite(color);
  const pieces = ctrl.shogiground.state.pieces;

  if (ctrl.practice) {
    if (ctrl.practice.hovering())
      return makeShapesFromUci(color, ctrl.practice.hovering().uci, "paleGreen", pieces);
    const hint = ctrl.practice.hinting();

    if (hint) {
      if (hint.mode === "move")
        return makeShapesFromUci(color, hint.uci, "paleGreen", pieces);
      else
        return [
          {
            orig:
              hint.uci[1] === "*" ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2),
            brush: "paleGreen",
          },
        ];
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const hovering = ctrl.explorer.hovering() || instance.hovering();
  const {
    eval: nEval = {},
    fen: nFen,
    ceval: nCeval,
    threat: nThreat,
  } = ctrl.node;

  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeShapesFromUci(color, ctrl.retro.showBadNode().uci, "paleRed", pieces, {
      lineWidth: 8,
    });
  }
  if (hovering && hovering.fen === nFen)
    shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, "paleGreen", pieces), );
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best)
      shapes = shapes.concat(
        makeShapesFromUci(rcolor, nEval.best, "paleGreen", pieces)
      );
    if (!hovering) {
      let nextBest = ctrl.nextNodeBest();
      if (!nextBest && instance.enabled() && nCeval)
        nextBest = nCeval.pvs[0].moves[0];
      if (nextBest)
        shapes = shapes.concat(makeShapesFromUci(color, nextBest, "paleGreen", pieces));
      if (
        instance.enabled() &&
        nCeval &&
        nCeval.pvs[1] &&
        !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)
      ) {
        nCeval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            shapes = shapes.concat(
              makeShapesFromUci(color, pv.moves[0], "paleGreen", pieces, {
                lineWidth: Math.round(12 - shift * 50), // 12 to 2
              })
            );
          }
        });
      }
    }
  }
  if (instance.enabled() && ctrl.threatMode() && nThreat) {
    const [pv0, ...pv1s] = nThreat.pvs;

    shapes = shapes.concat(
      makeShapesFromUci(
        rcolor,
        pv0.moves[0],
        pv1s.length > 0 ? "paleRed" : "red",
        pieces
      )
    );

    pv1s.forEach(function (pv) {
      const shift = winningChances.povDiff(rcolor, pv, pv0);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(
          makeShapesFromUci(rcolor, pv.moves[0], "paleRed", pieces, {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          })
        );
      }
    });
  }
  return shapes;
}
