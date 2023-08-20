import { parseUci, makeSquare, squareRank } from 'chessops/util';
import { isDrop } from 'chessops/types';
import { winningChances } from 'ceval';
import * as cg from 'chessground/types';
import { opposite } from 'chessground/util';
import { DrawModifiers, DrawShape } from 'chessground/draw';
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
  if (ctrl.showAutoShapes() && ctrl.showComputer()) {
    if (nEval.best) shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering && instance.multiPv()) {
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
  shapes = shapes.concat(annotationShapes(ctrl));

  if (ctrl.showAutoShapes() && ctrl.node.children.length > 1) {
    ctrl.node.children.forEach((node, i) => {
      const existing = shapes.find(s => s.orig === node.uci!.slice(0, 2) && s.dest === node.uci!.slice(2, 4));
      const symbol = node.glyphs?.[0]?.symbol;
      if (existing) {
        existing.brush = i === 0 ? 'purple' : existing.brush;
        if (i === ctrl.fork.selected()) {
          existing.modifiers ??= {};
          existing.modifiers.hilite = true;
        }
        if (symbol) existing.label = { text: symbol, fill: glyphColors[symbol] };
      } else
        shapes.push({
          orig: node.uci!.slice(0, 2) as Key,
          dest: node.uci?.slice(2, 4) as Key,
          brush: i === 0 ? 'purple' : 'pink',
          modifiers: { hilite: i === ctrl.fork.selected() },
          label: symbol ? { text: symbol, fill: glyphColors[symbol] } : undefined,
        });
    });
  }
  return shapes;
}

const glyphColors: { [k: string]: string } = {
  '??': '#df5353',
  '?': '#e69f00',
  '?!': '#56b4e9',
  '!': '#22ac38',
  '!!': '#168226',
  '!?': '#ea45d8',
};

function annotationShapes(ctrl: AnalyseCtrl): DrawShape[] {
  const shapes: DrawShape[] = [];
  const { uci, glyphs, san } = ctrl.node;
  if (ctrl.showMoveAnnotation() && uci && san && glyphs && glyphs.length > 0) {
    const move = parseUci(uci)!;
    const destSquare = san.startsWith('O-O') // castle, short or long
      ? squareRank(move.to) === 0 // white castle
        ? san.startsWith('O-O-O')
          ? 'c1'
          : 'g1'
        : san.startsWith('O-O-O')
        ? 'c8'
        : 'g8'
      : makeSquare(move.to);
    shapes.push({
      orig: destSquare,
      label: { text: glyphs[0].symbol, fill: glyphColors[glyphs[0].symbol] },
    });
  }
  return shapes;
}
