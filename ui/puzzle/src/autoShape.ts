import { CevalCtrl, winningChances } from 'ceval';
import { Api as SgApi } from 'shogiground/api';
import { DrawShape } from 'shogiground/draw';
import { opposite } from 'shogiground/util';
import { isDrop } from 'shogiops/types';
import { makeSquareName, parseUsi } from 'shogiops/util';
import { Vm } from './interfaces';

interface Opts {
  vm: Vm;
  ceval: CevalCtrl;
  ground: SgApi;
  nextNodeBest?: Usi;
  threatMode: boolean;
}

function makeAutoShapesFromUsi(
  usi: Usi,
  color: Color,
  brush: 'engine' | 'engineAlt' | 'engineThreat' | 'engineThreatAlt'
): DrawShape[] {
  const move = parseUsi(usi);
  if (!move) return [];
  if (isDrop(move))
    return [
      {
        orig: makeSquareName(move.to),
        dest: makeSquareName(move.to),
        brush,
      },
      {
        orig: makeSquareName(move.to),
        dest: makeSquareName(move.to),
        piece: {
          role: move.role,
          color: color,
          scale: 0.8,
        },
        brush: brush,
      },
    ];
  else {
    return [
      {
        orig: makeSquareName(move.from),
        dest: makeSquareName(move.to),
        brush: brush,
        description: move.promotion ? '+' : undefined,
      },
    ];
  }
}

export default function (opts: Opts): DrawShape[] {
  const n = opts.vm.node,
    hovering = opts.ceval.hovering(),
    color = opts.ground.state.activeColor,
    turnColor = opts.ground.state.turnColor;
  let shapes: DrawShape[] = [];
  if (hovering && hovering.sfen === n.sfen)
    shapes = shapes.concat(makeAutoShapesFromUsi(hovering.usi, turnColor, 'engine'));
  if (opts.vm.showAutoShapes() && opts.vm.showComputer()) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUsi(n.eval.best!, turnColor, 'engine'));
    if (!hovering) {
      const useCur = !opts.nextNodeBest && opts.ceval.enabled(),
        nextBest = useCur && n.ceval ? n.ceval.pvs[0].moves[0] : opts.nextNodeBest;
      if (nextBest)
        shapes = shapes.concat(
          makeAutoShapesFromUsi(nextBest, turnColor, useCur && n.ceval ? 'engine' : 'engineThreat')
        );
      if (
        opts.ceval.enabled() &&
        n.ceval &&
        n.ceval.pvs &&
        n.ceval.pvs[1] &&
        !(opts.threatMode && n.threat && n.threat.pvs[2])
      ) {
        n.ceval.pvs.forEach((pv, i) => {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color as Color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(makeAutoShapesFromUsi(pv.moves[0], turnColor, i > 0 ? 'engineAlt' : 'engine'));
        });
      }
    }
  }
  if (opts.ceval.enabled() && opts.threatMode && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUsi(n.threat.pvs[0].moves[0], turnColor, 'engineThreat'));
      n.threat.pvs.slice(1).forEach(pv => {
        const shift = winningChances.povDiff(opposite(color as Color), pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(makeAutoShapesFromUsi(pv.moves[0], turnColor, 'engineThreatAlt'));
      });
    } else shapes = shapes.concat(makeAutoShapesFromUsi(n.threat.pvs[0].moves[0], turnColor, 'engineThreat'));
  }
  return shapes;
}
