import { parseUci, makeSquare } from 'chessops/util';
import { isDrop, type Square } from 'chessops/types';
import { winningChances } from 'lib/ceval';
import { opposite } from '@lichess-org/chessground/util';
import type { DrawModifiers, DrawShape } from '@lichess-org/chessground/draw';
import { annotationShapes, analysisGlyphs } from 'lib/game/glyphs';
import type AnalyseCtrl from './ctrl';
import { isUci } from 'lib/game/chess';
import { parseFen } from 'chessops/fen';
import type { ServerEval } from 'lib/tree/types';
import { between, ray, knightAttacks } from 'chessops/attacks';

const pieceDrop = (key: Key, role: Role, color: Color): DrawShape => ({
  orig: key,
  piece: {
    color,
    role,
    scale: 0.8,
  },
  brush: 'green',
});

const MAX_MANEUVER_ARROWS = 3;

function interferingArrow(from: Square, to: Square, occupied: Uint8Array): boolean {
  if (from === to) return true; // Ignore null moves
  occupied[from] = 1;

  // Knight: check only the destination
  if (knightAttacks(from).has(to)) {
    if (occupied[to]) return true;
    occupied[to] = 1;
    return false;
  }

  // Sliding piece: check every square along the path
  const line = ray(from, to);
  if (line.has(to)) {
    for (const sq of between(from, to)) {
      if (occupied[sq]) return true;
      occupied[sq] = 1;
    }
    if (occupied[to]) return true;
    occupied[to] = 1;
    return false;
  }

  return true;
}

function drawManeuver(ctrl: AnalyseCtrl, color: Color, moves: Uci[], brush: string, shapes: DrawShape[]) {
  if (ctrl.showManeuverMoveArrowsProp()) {
    const maxPairs = Math.min(moves.length, MAX_MANEUVER_ARROWS * 2);
    const occupied = new Uint8Array(64);

    for (let i = 0; i < maxPairs; i += 2) {
      const uci = moves[i];
      const move = parseUci(uci);
      if (!move) break;

      if (i > 0) {
        const prevMove = parseUci(moves[i - 2])!;
        if (prevMove.to !== (isDrop(move) ? -1 : move.from)) break;
      }

      if (isDrop(move)) {
        if (occupied[move.to]) break;
        occupied[move.to] = 1;
      } else if (interferingArrow(move.from, move.to, occupied)) break;

      makeShapesFromUci(color, uci, brush).forEach(s => shapes.push(s));
    }
  } else if (moves[0]) makeShapesFromUci(color, moves[0], brush).forEach(s => shapes.push(s));
}

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
  const { eval: nEval = {} as Partial<ServerEval>, fen: nFen, ceval: nCeval, threat: nThreat } = ctrl.node;

  let hovering = ctrl.explorer.hovering();

  if (!hovering || hovering.fen !== nFen) {
    ctrl.explorer.hovering(null);
    hovering = ctrl.ceval.hovering();
  }

  let shapes: DrawShape[] = [],
    badNode;
  if (ctrl.retro && (badNode = ctrl.retro.showBadNode())) {
    return makeShapesFromUci(color, badNode.uci!, 'paleRed', {
      lineWidth: 8,
    });
  }
  if (hovering?.fen === nFen) shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, 'paleBlue'));
  ctrl.fork.hover(hovering?.uci);

  if (ctrl.showBestMoveArrows() && ctrl.showAnalysis()) {
    if (isUci(nEval.best)) shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering && ctrl.ceval.search.multiPv) {
      const bestPvMoves = ctrl.isCevalAllowed() && nCeval ? nCeval.pvs[0]?.moves : undefined;
      const nextBest = bestPvMoves?.[0] || ctrl.nextNodeBest();

      if (nextBest) {
        drawManeuver(ctrl, color, bestPvMoves || [nextBest], 'paleBlue', shapes);
      }

      if (
        ctrl.isCevalAllowed() &&
        nCeval?.pvs[1] &&
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
  if (ctrl.isCevalAllowed() && ctrl.threatMode() && nThreat) {
    const [pv0, ...pv1s] = nThreat.pvs;
    const brush = pv1s.length > 0 ? 'paleRed' : 'red';

    drawManeuver(ctrl, rcolor, pv0.moves, brush, shapes);

    pv1s.forEach(function (pv) {
      const shift = winningChances.povDiff(rcolor, pv0, pv);
      if (shift >= 0 && shift < 0.2) {
        shapes = shapes.concat(
          makeShapesFromUci(rcolor, pv.moves[0], 'paleRed', {
            lineWidth: Math.round(11 - shift * 45), // 11 to 2
          }),
        );
      }
    });
  }
  if (ctrl.showMoveAnnotationsOnBoard()) shapes = shapes.concat(annotationShapes(ctrl.node));
  if (ctrl.showVariationArrows()) hiliteVariations(ctrl, shapes);

  if (ctrl.isCevalAllowed()) {
    const parsed = parseFen(nFen);
    if ('error' in parsed) return shapes;
    const { board, epSquare, castlingRights } = parsed.value;

    const addAnalysis = (orig: Key, type: keyof typeof analysisGlyphs) => {
      const idx = shapes.filter(s => s.orig === orig && s.customSvg).length;
      shapes.push({
        orig,
        customSvg: { html: analysisGlyphs[type](idx) },
      });
    };

    if (ctrl.motifEnabled()) {
      ctrl.motif.detectPins(board).forEach(p => addAnalysis(makeSquare(p.pinned) as Key, 'pin'));
      ctrl.motif
        .detectUndefended(board, epSquare)
        .forEach(u => addAnalysis(makeSquare(u.square) as Key, 'undefended'));
      ctrl.motif
        .detectCheckable(board, epSquare, castlingRights)
        .forEach(s => addAnalysis(makeSquare(s.king) as Key, 'checkable'));
    }
  }

  return shapes;
}

function hiliteVariations(ctrl: AnalyseCtrl, autoShapes: DrawShape[]) {
  const visible = ctrl.visibleChildren();
  if (visible.length < 2) return;
  ctrl.chessground.state.drawable.brushes['variation'] = {
    key: 'variation',
    color: 'white',
    opacity: ctrl.variationArrowOpacity() || 0,
    lineWidth: 12,
  };
  const chap = ctrl.study?.data.chapter;
  const isGamebookEditor = chap?.gamebook && !ctrl.study?.gamebookPlay;
  for (const [i, node] of visible.entries()) {
    const existing = autoShapes.find(s => s.orig + s.dest === node.uci);
    if (existing) existing.modifiers = { hilite: i === ctrl.fork.selectedIndex ? 'white' : undefined };
    else
      autoShapes.push({
        orig: node.uci!.slice(0, 2) as Key,
        dest: node.uci?.slice(2, 4) as Key,
        brush: !isGamebookEditor ? 'variation' : i === 0 ? 'paleGreen' : 'paleRed',
        modifiers: { hilite: i === ctrl.fork.selectedIndex ? '#3291ff' : '#aaa' },
        below: true,
      });
  }
}
