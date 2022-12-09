import { winningChances, CevalCtrl } from 'ceval';
import { DrawModifiers, DrawShape } from 'chessground/draw';
import { Vm } from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { opposite, parseUci, makeSquare } from 'chessops/util';
import { NormalMove } from 'chessops/types';

interface Opts {
  vm: Vm;
  ceval: CevalCtrl;
  ground: CgApi;
  nextNodeBest?: Uci;
  threatMode: boolean;
}

function makeAutoShapesFromUci(color: Color, uci: Uci, brush: string, modifiers?: DrawModifiers): DrawShape[] {
  const move = parseUci(uci)! as NormalMove; // no crazyhouse
  const to = makeSquare(move.to);
  return [
    { orig: makeSquare(move.from), dest: to, brush, modifiers },
    ...(move.promotion ? [{ orig: to, piece: { color, role: move.promotion, scale: 0.8 }, brush: 'green' }] : []),
  ];
}

export default function (opts: Opts): DrawShape[] {
  const n = opts.vm.node,
    hovering = opts.ceval.hovering(),
    color = n.fen.includes(' w ') ? 'white' : 'black';
  let shapes: DrawShape[] = [];
  if (hovering && hovering.fen === n.fen)
    shapes = shapes.concat(makeAutoShapesFromUci(color, hovering.uci, 'paleBlue'));
  if (opts.vm.showAutoShapes() && opts.vm.showComputer()) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUci(color, n.eval.best!, 'paleGreen'));
    if (!hovering) {
      let nextBest: Uci | undefined = opts.nextNodeBest;
      if (!nextBest && opts.ceval.enabled() && n.ceval) nextBest = n.ceval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextBest, 'paleBlue'));
      if (
        opts.ceval.enabled() &&
        n.ceval &&
        n.ceval.pvs &&
        n.ceval.pvs[1] &&
        !(opts.threatMode && n.threat && n.threat.pvs[2])
      ) {
        n.ceval.pvs.forEach(function (pv) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(
            makeAutoShapesFromUci(color, pv.moves[0], 'paleGrey', {
              lineWidth: Math.round(12 - shift * 50), // 12 to 2
            })
          );
        });
      }
    }
  }
  if (opts.ceval.enabled() && opts.threatMode && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], 'paleRed'));
      n.threat.pvs.slice(1).forEach(function (pv) {
        const shift = winningChances.povDiff(opposite(color), pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(
          makeAutoShapesFromUci(opposite(color), pv.moves[0], 'paleRed', {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          })
        );
      });
    } else shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], 'red'));
  }
  return shapes;
}
