import { winningChances, scan2uci } from 'ceval';
import { decomposeUci } from 'draughts';
import { DrawShape } from 'draughtsground/draw';
import { Vm } from './interfaces';
import { Api as CgApi } from 'draughtsground/api';
import { CevalCtrl } from 'ceval';
import { opposite } from 'draughtsground/util';

interface Opts {
  vm: Vm;
  ceval: CevalCtrl;
  ground: CgApi;
  nextNodeBest?: Uci;
  threatMode: boolean;
}

function makeAutoShapesFromUci(uci: Uci, brush: string, modifiers?: any): DrawShape[] {
  const moves = decomposeUci(scan2uci(uci));
  if (moves.length == 1) return [{
    orig: moves[0],
    brush,
    modifiers
  }];
  const shapes: DrawShape[] = new Array<DrawShape>();
  for (let i = 0; i < moves.length - 1; i++)
    shapes.push({
      orig: moves[i],
      dest: moves[i + 1],
      brush,
      modifiers
    });
  return shapes;
}

export default function(opts: Opts): DrawShape[] {
  const n = opts.vm.node,
  hovering = opts.ceval.hovering(),
  color = opts.ground.state.movable.color;
  let shapes: DrawShape[] = [];
  if (hovering && hovering.fen === n.fen) shapes = shapes.concat(makeAutoShapesFromUci(hovering.uci, 'paleBlue'));
  if (opts.vm.showAutoShapes() && opts.vm.showComputer()) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUci(n.eval.best!, 'paleGreen'));
    if (!hovering) {
      let nextBest: Uci | undefined = opts.nextNodeBest;
      const ghostNode = n.displayPly && n.displayPly !== n.ply && opts.vm.nodeList.length > 1;
      if (!nextBest && opts.ceval.enabled()) {
        const prevCeval = ghostNode ? opts.vm.nodeList[opts.vm.nodeList.length - 2].ceval : undefined;
        if (ghostNode && prevCeval && prevCeval.pvs[0].moves[0].indexOf('x') !== -1 && n.uci) {
          const ucis = n.uci.match(/.{1,2}/g);
          if (!!ucis) {
            const sans = ucis.slice(0, ucis.length - 1).map(uci => parseInt(uci).toString()).join('x');
            nextBest = prevCeval.pvs[0].moves[0].slice(sans.length + 1);
          }
        } else if (n.ceval)
          nextBest = n.ceval.pvs[0].moves[0];
      }
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(nextBest, 'paleBlue'));
      if (!ghostNode && opts.ceval.enabled() && n.ceval && n.ceval.pvs && n.ceval.pvs[1] && !(opts.threatMode && n.threat && n.threat.pvs[2])) {
        n.ceval.pvs.forEach(function(pv) {
          if (pv.moves[0] === nextBest) return;
          var shift = winningChances.povDiff(color as Color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(makeAutoShapesFromUci(pv.moves[0], 'paleGrey', {
            lineWidth: Math.round(12 - shift * 50) // 12 to 2
          }));
        });
      }
    }
  }
  if (opts.ceval.enabled() && opts.threatMode && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUci(n.threat.pvs[0].moves[0], 'paleRed'));
      n.threat.pvs.slice(1).forEach(function(pv) {
        const shift = winningChances.povDiff(opposite(color as Color), pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(makeAutoShapesFromUci(pv.moves[0], 'paleRed', {
          lineWidth: Math.round(11 - shift * 45) // 11 to 2
        }));
      });
    } else
    shapes = shapes.concat(makeAutoShapesFromUci(n.threat.pvs[0].moves[0], 'red'));
  }
  return shapes;
}
