import { CevalCtrl, winningChances } from 'ceval';
import { DrawShape } from 'shogiground/draw';
import { Vm } from './interfaces';
import { Api as SgApi } from 'shogiground/api';
import { opposite } from 'shogiground/util';
import { Pieces } from 'shogiground/types';
import { isDrop } from 'shogiops/types';
import { makeSquare, parseUsi } from 'shogiops/util';
import { defined } from 'common/common';

interface Opts {
  vm: Vm;
  ceval: CevalCtrl;
  ground: SgApi;
  nextNodeBest?: Usi;
  threatMode: boolean;
}

function makeAutoShapesFromUsi(usi: Usi, color: Color, brush: string, pieces?: Pieces): DrawShape[] {
  const move = parseUsi(usi);
  if (!move) return [];
  if (isDrop(move))
    return [
      {
        orig: makeSquare(move.to) as Key,
        dest: makeSquare(move.to) as Key,
        brush,
      },
      {
        orig: makeSquare(move.to) as Key,
        dest: makeSquare(move.to) as Key,
        piece: {
          role: move.role,
          color: color,
        },
        brush: brush,
      },
    ];
  else {
    const shapes: DrawShape[] = [
      {
        orig: makeSquare(move.from) as Key,
        dest: makeSquare(move.to) as Key,
        brush: brush,
      },
    ];
    const pieceToPromote = move.promotion ? pieces?.get(usi.slice(0, 2) as Key) : undefined;
    if (defined(pieceToPromote)) {
      shapes.push({
        orig: makeSquare(move.to) as Key,
        dest: makeSquare(move.to) as Key,
        piece: {
          color: pieceToPromote.color,
          role: pieceToPromote.role,
          scale: 0.8,
        },
        brush: 'green',
      });
    }

    return shapes;
  }
}

export default function (opts: Opts): DrawShape[] {
  const n = opts.vm.node,
    hovering = opts.ceval.hovering(),
    color = opts.ground.state.activeColor,
    turnColor = opts.ground.state.turnColor;
  let shapes: DrawShape[] = [];
  if (hovering && hovering.sfen === n.sfen)
    shapes = shapes.concat(makeAutoShapesFromUsi(hovering.usi, turnColor, 'paleBlue'));
  if (opts.vm.showAutoShapes() && opts.vm.showComputer()) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUsi(n.eval.best!, turnColor, 'paleGreen'));
    if (!hovering) {
      let nextBest: Usi | undefined = opts.nextNodeBest;
      if (!nextBest && opts.ceval.enabled() && n.ceval) nextBest = n.ceval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUsi(nextBest, turnColor, 'paleBlue'));
      if (
        opts.ceval.enabled() &&
        n.ceval &&
        n.ceval.pvs &&
        n.ceval.pvs[1] &&
        !(opts.threatMode && n.threat && n.threat.pvs[2])
      ) {
        n.ceval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color as Color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(makeAutoShapesFromUsi(pv.moves[0], turnColor, 'paleGrey', opts.ground.state.pieces));
        });
      }
    }
  }
  if (opts.ceval.enabled() && opts.threatMode && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUsi(n.threat.pvs[0].moves[0], turnColor, 'paleRed'));
      n.threat.pvs.slice(1).forEach(function (pv) {
        const shift = winningChances.povDiff(opposite(color as Color), pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(makeAutoShapesFromUsi(pv.moves[0], turnColor, 'paleRed', opts.ground.state.pieces));
      });
    } else shapes = shapes.concat(makeAutoShapesFromUsi(n.threat.pvs[0].moves[0], turnColor, 'red'));
  }
  return shapes;
}
