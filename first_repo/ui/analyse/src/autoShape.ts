import { parseUci, makeSquare } from 'chessops/util';
import { isDrop } from 'chessops/types';
import { winningChances } from 'ceval';
import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { DrawShape } from 'chessground/draw';
import AnalyseCtrl from './ctrl';

function pieceDrop(key: cg.Key, role: cg.Role, color: Color): DrawShape {
  return {
    orig: key,
    piece: {
      color,
      role,
      scale: 0.8,
    },
    brush: 'green',
  };
}

export function makeShapesFromUci(color: Color, uci: Uci, brush: string, modifiers?: any): DrawShape[] {
  const move = parseUci(uci)!;
  const to = makeSquare(move.to);
  if (isDrop(move)) return [{ orig: to, brush }, pieceDrop(to, move.role, color)];

  const shapes: DrawShape[] = [
    {
      orig: makeSquare(move.from),
      dest: to,
      brush,
      modifiers,
    },
  ];
  if (move.promotion) shapes.push(pieceDrop(to, move.promotion, color));
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = ctrl.node.fen.includes(' w ') ? 'white' : 'black';
  const rcolor = opposite(color);
  if (ctrl.practice) {
    if (ctrl.practice.hovering()) return makeShapesFromUci(color, ctrl.practice.hovering().uci, 'green');
    const hint = ctrl.practice.hinting();
    if (hint) {
      if (hint.mode === 'move') return makeShapesFromUci(color, hint.uci, 'paleBlue');
      else
        return [
          {
            orig: hint.uci[1] === '@' ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2),
            brush: 'paleBlue',
          },
        ];
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const hovering = ctrl.explorer.hovering() || instance.hovering();
  const { eval: nEval = {} as Partial<Tree.ServerEval>, fen: nFen, ceval: nCeval, threat: nThreat } = ctrl.node;

  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeShapesFromUci(color, ctrl.retro.showBadNode().uci, 'paleRed', {
      lineWidth: 8,
    });
  }
  if (hovering && hovering.fen === nFen) shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, 'paleBlue'));
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best) shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering) {
      let nextBest = ctrl.nextNodeBest();
      if (!nextBest && instance.enabled() && nCeval) nextBest = nCeval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeShapesFromUci(color, nextBest, 'paleBlue'));
      if (instance.enabled() && nCeval && nCeval.pvs[1] && !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)) {
        nCeval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            shapes = shapes.concat(
              makeShapesFromUci(color, pv.moves[0], 'paleGrey', {
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

    shapes = shapes.concat(makeShapesFromUci(rcolor, pv0.moves[0], pv1s.length > 0 ? 'paleRed' : 'red'));

    pv1s.forEach(function (pv) {
      const shift = winningChances.povDiff(rcolor, pv, pv0);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(
          makeShapesFromUci(rcolor, pv.moves[0], 'paleRed', {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          })
        );
      }
    });
  }
  return shapes;
}
