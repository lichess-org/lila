import { winningChances } from 'ceval';
import { decomposeUci } from 'draughts';
import { opposite } from 'draughtsground/util';
import { DrawShape } from 'draughtsground/draw';
import AnalyseCtrl from './ctrl';

export function makeShapesFromUci(uci: Uci, brush: string, modifiers?: any): DrawShape[] {
  const moves = decomposeUci(uci);
  const shapes: DrawShape[] = new Array<DrawShape>();
  for (let i = 0; i < moves.length; i++) {
    if (i + 1 >= moves.length) {
      shapes.push({
        orig: moves[i],
        brush,
        modifiers
      });
      break;
    } else
      shapes.push({
        orig: moves[i],
        dest: moves[i + 1],
        brush,
        modifiers
      });
  }
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color: Color = ctrl.draughtsground.state.movable.color as Color;
  const rcolor: Color = opposite(color);
  if (ctrl.practice) {
    if (ctrl.practice.hovering()) return makeShapesFromUci(ctrl.practice.hovering().uci, 'green');
    const hint = ctrl.practice.hinting();
    if (hint) {
      if (hint.mode === 'move') return makeShapesFromUci(hint.uci, 'paleBlue');
      else return [{
        orig: hint.uci[1] === '@' ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2),
        brush: 'paleBlue'
      }];
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const hovering = ctrl.explorer.hovering() || instance.hovering();
  const {
    eval: nEval = {},
    fen: nFen,
    ceval: nCeval,
    threat: nThreat
  } = ctrl.node;

  let shapes: DrawShape[] = [];
  if (ctrl.retro && ctrl.retro.showBadNode()) {
    return makeShapesFromUci(ctrl.retro.showBadNode().uci, 'paleRed', {
      lineWidth: 8
    });
  }
  if (hovering && hovering.fen === nFen) shapes = shapes.concat(makeShapesFromUci(hovering.uci, 'paleBlue'));
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best) shapes = shapes.concat(makeShapesFromUci(nEval.best, 'paleGreen'));
    if (!hovering) {
      let nextBest = ctrl.nextNodeBest();
      if (!nextBest && instance.enabled() && nCeval) nextBest = nCeval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeShapesFromUci(nextBest, 'paleBlue'));
      if (instance.enabled() && nCeval && nCeval.pvs[1] && !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)) {
        nCeval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            shapes = shapes.concat(makeShapesFromUci(pv.moves[0], 'paleGrey', {
              lineWidth: Math.round(12 - shift * 50) // 12 to 2
            }));
          }
        });
      }
    }
  }
  if (instance.enabled() && ctrl.threatMode() && nThreat) {
    const [pv0, ...pv1s] = nThreat.pvs;

    shapes = shapes.concat(makeShapesFromUci(pv0.moves[0],
      pv1s.length > 0 ? 'paleRed' : 'red'));

    pv1s.forEach(function (pv) {
      const shift = winningChances.povDiff(rcolor, pv, pv0);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(makeShapesFromUci(pv.moves[0], 'paleRed', {
          lineWidth: Math.round(11 - shift * 45) // 11 to 2
        }));
      }
    });
  }
  return shapes;
}
