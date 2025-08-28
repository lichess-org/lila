import { winningChances } from 'lib/ceval/ceval';
import { annotationShapes } from 'lib/game/glyphs';
import type { DrawModifiers, DrawShape } from '@lichess-org/chessground/draw';
import { opposite, parseUci, makeSquare } from 'chessops/util';
import type { NormalMove } from 'chessops/types';
import type PuzzleCtrl from './ctrl';

function makeAutoShapesFromUci(
  color: Color,
  uci: Uci,
  brush: string,
  modifiers?: DrawModifiers,
): DrawShape[] {
  const move = parseUci(uci)! as NormalMove; // no crazyhouse
  const to = makeSquare(move.to);
  return [
    { orig: makeSquare(move.from), dest: to, brush, modifiers },
    ...(move.promotion
      ? [{ orig: to, piece: { color, role: move.promotion, scale: 0.8 }, brush: 'green' }]
      : []),
  ];
}

export default function (ctrl: PuzzleCtrl): DrawShape[] {
  const n = ctrl.node,
    hovering = ctrl.ceval.hovering(),
    color = n.fen.includes(' w ') ? 'white' : 'black';
  let shapes: DrawShape[] = [];
  if (hovering && hovering.fen === n.fen)
    shapes = shapes.concat(makeAutoShapesFromUci(color, hovering.uci, 'paleBlue'));
  if (ctrl.showAnalysis() && ctrl.ceval.storedPv() > 0) {
    if (n.eval) shapes = shapes.concat(makeAutoShapesFromUci(color, n.eval.best!, 'paleGreen'));
    if (!hovering) {
      let nextBest: Uci | undefined = ctrl.nextNodeBest();
      if (!nextBest && ctrl.cevalEnabled() && n.ceval) nextBest = n.ceval.pvs[0].moves[0];
      if (nextBest) shapes = shapes.concat(makeAutoShapesFromUci(color, nextBest, 'paleBlue'));
      if (ctrl.cevalEnabled() && n.ceval?.pvs?.[1] && !(ctrl.threatMode() && n.threat?.pvs[2])) {
        n.ceval.pvs.forEach(pv => {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, n.ceval!.pvs[0], pv);
          if (shift > 0.2 || isNaN(shift) || shift < 0) return;
          shapes = shapes.concat(
            makeAutoShapesFromUci(color, pv.moves[0], 'paleGrey', {
              lineWidth: Math.round(12 - shift * 50), // 12 to 2
            }),
          );
        });
      }
    }
  }
  if (ctrl.cevalEnabled() && ctrl.threatMode() && n.threat) {
    if (n.threat.pvs[1]) {
      shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], 'paleRed'));
      n.threat.pvs.slice(1).forEach(pv => {
        const shift = winningChances.povDiff(opposite(color), pv, n.threat!.pvs[0]);
        if (shift > 0.2 || isNaN(shift) || shift < 0) return;
        shapes = shapes.concat(
          makeAutoShapesFromUci(opposite(color), pv.moves[0], 'paleRed', {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          }),
        );
      });
    } else shapes = shapes.concat(makeAutoShapesFromUci(opposite(color), n.threat.pvs[0].moves[0], 'red'));
  }
  const feedback = feedbackAnnotation(n);
  const hint =
    ctrl.hintSquare() !== undefined ? { orig: makeSquare(ctrl.hintSquare()!), brush: 'green' } : undefined;
  return [
    ...shapes,
    ...annotationShapes(n),
    ...(feedback ? annotationShapes(feedback) : []),
    ...(hint ? [hint] : []),
  ];
}

function feedbackAnnotation(n: Tree.Node): Tree.Node | undefined {
  let glyph: Tree.Glyph | undefined;
  switch (n.puzzle) {
    case 'good':
    case 'win':
      glyph = { id: 7, name: 'good', symbol: '✓' };
      break;
    case 'fail':
      glyph = { id: 4, name: 'fail', symbol: '✗' };
  }
  return glyph && { ...n, glyphs: [glyph] };
}
