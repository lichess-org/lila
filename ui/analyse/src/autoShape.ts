import { parseUci, makeSquare } from 'chessops/util';
import { isDrop } from 'chessops/types';
import { winningChances } from 'ceval';
import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { DrawModifiers, DrawShape } from 'chessground/draw';
import { annotationShapes } from 'chess/glyphs';
import AnalyseCtrl from './ctrl';

const pieceDrop = (key: cg.Key, role: cg.Role, color: Color): DrawShape => ({
  orig: key,
  piece: {
    color,
    role,
    scale: 0.8,
  },
  brush: 'green',
});

const findShape = (uci?: Uci, shapes?: Tree.Shape[]) =>
  ((shapes ?? []) as DrawShape[]).find(s => s.orig === uci?.slice(0, 2) && s.dest === uci?.slice(2, 4));

export function makeShapesFromUci(
  color: Color,
  uci: Uci,
  brush: string,
  modifiers?: DrawModifiers,
): DrawShape[] {
  if (uci === 'Current Position') return [];
  const move = parseUci(uci)!;
  const to = makeSquare(move.to);
  if (isDrop(move)) return [{ orig: to, brush }, pieceDrop(to, move.role, color)];

  const shapes: DrawShape[] = [{ orig: makeSquare(move.from), dest: to, brush, modifiers }];
  if (move.promotion) shapes.push(pieceDrop(to, move.promotion, color));
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = ctrl.node.fen.includes(' w ') ? 'white' : 'black';
  const rcolor = opposite(color);
  if (ctrl.practice) {
    const hovering = ctrl.practice.hovering();
    if (hovering) return makeShapesFromUci(color, hovering.uci, 'green');
    const hint = ctrl.practice.hinting();
    if (hint) {
      if (hint.mode === 'move') return makeShapesFromUci(color, hint.uci, 'paleBlue');
      else
        return [
          {
            orig: (hint.uci[1] === '@' ? hint.uci.slice(2, 4) : hint.uci.slice(0, 2)) as Key,
            brush: 'paleBlue',
          },
        ];
    }
    return [];
  }
  const instance = ctrl.getCeval();
  const {
    eval: nEval = {} as Partial<Tree.ServerEval>,
    fen: nFen,
    ceval: nCeval,
    threat: nThreat,
  } = ctrl.node;

  let hovering = ctrl.explorer.hovering();

  if (!hovering || hovering.fen !== nFen) {
    ctrl.explorer.hovering(null);
    hovering = instance.hovering();
  }

  let shapes: DrawShape[] = [],
    badNode;
  if (ctrl.retro && (badNode = ctrl.retro.showBadNode())) {
    return makeShapesFromUci(color, badNode.uci!, 'paleRed', {
      lineWidth: 8,
    });
  }
  ctrl.fork.hover(hovering?.uci);
  if (hovering?.fen === nFen) shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, 'paleBlue'));

  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best && !ctrl.showVariationArrows())
      shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering && instance.search.multiPv) {
      const nextBest = instance.enabled() && nCeval ? nCeval.pvs[0].moves[0] : ctrl.nextNodeBest();
      if (nextBest) shapes = shapes.concat(makeShapesFromUci(color, nextBest, 'paleBlue', undefined));
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
              makeShapesFromUci(color, pv.moves[0], 'paleGrey', {
                lineWidth: Math.round(12 - shift * 50), // 12 to 2
              }),
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
          }),
        );
      }
    });
  }
  if (ctrl.showMoveAnnotation()) shapes = shapes.concat(annotationShapes(ctrl.node));
  if (ctrl.showVariationArrows()) hiliteVariations(ctrl, shapes);
  return shapes;
}

function hiliteVariations(ctrl: AnalyseCtrl, autoShapes: DrawShape[]) {
  const chap = ctrl.study?.data.chapter;
  const isGamebookEditor = chap?.gamebook && !ctrl.study?.gamebookPlay;

  for (const [i, node] of ctrl.node.children.entries()) {
    if (node.comp && !ctrl.showComputer()) continue;
    const userShape = findShape(node.uci, ctrl.node.shapes);

    if (userShape && i === ctrl.fork.selected()) autoShapes.push({ ...userShape }); // so we can hilite it

    const existing = findShape(node.uci, autoShapes);
    const brush = isGamebookEditor
      ? i === 0
        ? 'paleGreen'
        : 'paleRed'
      : existing
      ? existing.brush
      : 'white';
    if (existing) {
      if (i === ctrl.fork.selected()) {
        existing.brush = brush;
        if (!existing.modifiers) existing.modifiers = {};
        existing.modifiers.hilite = true;
      }
    } else if (!userShape) {
      autoShapes.push({
        orig: node.uci!.slice(0, 2) as Key,
        dest: node.uci?.slice(2, 4) as Key,
        brush,
        modifiers: { hilite: i === ctrl.fork.selected() },
      });
    }
  }
}
