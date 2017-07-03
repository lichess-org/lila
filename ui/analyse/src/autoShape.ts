import { winningChances } from 'ceval';
import { decomposeUci, sanToRole } from 'chess';
import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { DrawShape } from 'chessground/draw';
import AnalyseController from './ctrl';

function pieceDrop(key: cg.Key, role: cg.Role, color: Color): DrawShape {
  return {
    orig: key,
    piece: {
      color,
      role,
      scale: 0.8
    },
    brush: 'green'
  };
}

export function makeAutoShapesFromUci(color: Color, uci: Uci, brush: string, modifiers?: any): DrawShape[] {
  const move = decomposeUci(uci);
  if (uci[1] === '@') return [{
    orig: move[1],
    brush
  },
  pieceDrop(move[1] as cg.Key, sanToRole[uci[0].toUpperCase()], color)
  ];
  const shapes: DrawShape[] = [{
    orig: move[0],
    dest: move[1],
    brush,
    modifiers
  }];
  if (move[2]) shapes.push(pieceDrop(move[1]!, move[2] as cg.Role, color));
  return shapes;
}

export function compute(ctrl: AnalyseController): DrawShape[] {
  const color: Color = ctrl.chessground.state.movable.color as Color;
  const rcolor: Color = opposite(color);
  if (ctrl.practice) {
    if (ctrl.practice.hovering()) return makeAutoShapesFromUci(color, ctrl.practice.hovering().uci, 'green');
    const hint = ctrl.practice.hinting();
    if (hint) {
      if (hint.mode === 'move') return makeAutoShapesFromUci(color, hint.uci, 'paleBlue');
      else return [{
        orig: hint.uci[1] === '@' ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2),
        brush: 'paleBlue'
      }];
    }
    return [];
  }
  const instance = ctrl.getCeval(),
  n = ctrl.node,
  hovering = ctrl.explorer.hovering() || instance.hovering();
  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeAutoShapesFromUci(color, ctrl.retro.showBadNode().uci, 'paleRed', {
      lineWidth: 8
    });
  }
  if (hovering && hovering.fen === n.fen) shapes = shapes.concat(makeAutoShapesFromUci(color, hovering.uci, 'paleBlue'));
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (n.eval && n.eval.best) shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.eval.best, 'paleGreen'));
    if (!hovering) {
      let nextBest = ctrl.nextNodeBest();
      if (!nextBest && instance.enabled() && n.ceval) nextBest = n.ceval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextBest, 'paleBlue'));
      if (instance.enabled() && n.ceval && n.ceval.pvs[1] && !(ctrl.threatMode() && n.threat && n.threat.pvs[2])) {
        n.ceval.pvs.forEach(function(pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(makeAutoShapesFromUci(color, pv.moves[0], 'paleGrey', {
            lineWidth: Math.round(12 - shift * 50) // 12 to 2
          }));
        });
      }
    }
  }
  if (instance.enabled() && ctrl.threatMode() && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.threat.pvs[0].moves[0], 'paleRed'));
      n.threat.pvs.slice(1).forEach(function(pv) {
        const shift = winningChances.povDiff(rcolor, pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(makeAutoShapesFromUci(rcolor, pv.moves[0], 'paleRed', {
          lineWidth: Math.round(11 - shift * 45) // 11 to 2
        }));
      });
    } else
    shapes = shapes.concat(makeAutoShapesFromUci(rcolor, n.threat.pvs[0].moves[0], 'red'));
  }
  return shapes;
}
