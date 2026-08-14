import type { DrawModifiers, DrawShape } from '@lichess-org/chessground/draw';
import { opposite } from '@lichess-org/chessground/util';
import { between, ray, knightAttacks } from 'chessops/attacks';
import { parseFen } from 'chessops/fen';
import { isDrop, type Square } from 'chessops/types';
import { parseUci, makeSquare, squareFile, squareRank } from 'chessops/util';

import { defined } from 'lib';
import { winningChances } from 'lib/ceval';
import { fenColor } from 'lib/game';
import { isUci } from 'lib/game/chess';
import { annotationShapes, analysisGlyphs } from 'lib/game/glyphs';
import type { ServerEval, TreeNode } from 'lib/tree/types';

import type AnalyseCtrl from './ctrl';

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
  if (ctrl.settings.showManeuverMoveArrows) {
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

// Numerals are drawn inside a 100 unit box that covers one square, so these are all
// hundredths of a square.
const RANK_FONT_SCALE = 2.2; // font-size tracks the brush's lineWidth...
const RANK_FONT_MIN = 15; // ...but the faintest lines have an 8px head, so it is floored
const RANK_OUTLINE_WIDTH = 1.35; // ~1px on a 600px board

// chessground 10.1.1 svg.ts, in 64ths of a square
const CG_LABEL_COORDS_BACK = 33; // labelCoords, back from the destination
const CG_ARROW_MARGIN = 10; // arrowMargin, how far short of it the line stops
const CG_PALE_BLUE_LINE_WIDTH = 15; // brushes.paleBlue.lineWidth, ie the rank 1 arrow
// ...and in stroke-widths, of which the head's marker path `M0,0 V4 L3,2 Z` is 3 long.
// Its refX is what catches you out: the point pinned to the line's end is inside the
// triangle rather than at the tip.
const CG_MARKER_REF_X = 2.05;
const RANK_HEAD_FRACTION = 1.35; // where the numeral sits in those units: 0 base, 3 tip

// Label anchor to RANK_HEAD_FRACTION along the head, in hundredths of a square. A shared
// destination needs no special case, since labelCoords and arrowMargin both grow by
// 10/64 and cancel; a knight boxed in by a neighbour does not, and sits behind its head.
const headOffset = (lineWidth: number): number =>
  ((CG_LABEL_COORDS_BACK - CG_ARROW_MARGIN + (RANK_HEAD_FRACTION - CG_MARKER_REF_X) * lineWidth) / 64) * 100;

function rankShape(rank: number, uci: Uci, lineWidth: number, flip: boolean): DrawShape | undefined {
  const move = parseUci(uci);
  if (!move || isDrop(move)) return undefined; // a drop is a circle, not an arrow
  const { from, to } = move;
  // chessground user coords: x grows with file, y grows *downward*, and flipping the
  // board negates both
  const dx = squareFile(to) - squareFile(from),
    dy = squareRank(from) - squareRank(to),
    mag = Math.sqrt(dx * dx + dy * dy) || 1,
    off = headOffset(lineWidth) * (flip ? -1 : 1),
    x = 50 + (dx / mag) * off,
    y = 50 + (dy / mag) * off,
    size = Math.max(RANK_FONT_SCALE * lineWidth, RANK_FONT_MIN),
    at = (n: number) => n.toFixed(1);

  // `fill` must be a css declaration, not a presentation attribute, or var() will not
  // substitute. Opacity is pinned so the numeral survives the faintest arrows.
  const style =
    `fill:var(--c-font);fill-opacity:1;stroke:#000;stroke-width:${RANK_OUTLINE_WIDTH};` +
    `stroke-opacity:1;paint-order:stroke`;

  return {
    orig: makeSquare(from),
    dest: makeSquare(to),
    customSvg: {
      html:
        `<text x="${at(x)}" y="${at(y)}" font-family="Noto Sans,Arial,sans-serif"` +
        ` font-weight="bold" font-size="${at(size)}" style="${style}"` +
        ` text-anchor="middle" dominant-baseline="central">${rank}</text>`,
      center: 'label',
    },
  };
}

export function makeShapesFromUci(
  color: Color,
  uci: Uci | undefined,
  brush: string,
  modifiers?: DrawModifiers,
): DrawShape[] {
  if (!uci || uci === 'Current Position') return [];
  const move = parseUci(uci)!;
  const to = makeSquare(move.to);
  if (isDrop(move)) return [{ orig: to, brush }, pieceDrop(to, move.role, color)];

  const shapes: DrawShape[] = [{ orig: makeSquare(move.from), dest: to, brush, modifiers }];
  if (move.promotion) shapes.push(pieceDrop(to, move.promotion, color));
  return shapes;
}

export function compute(ctrl: AnalyseCtrl): DrawShape[] {
  const color = fenColor(ctrl.node.fen);
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

  let shapes: DrawShape[] = [];
  let badNode: TreeNode | undefined;
  if ((badNode = ctrl.retro?.showBadNode()) && badNode.uci) {
    return makeShapesFromUci(color, badNode.uci, 'paleRed', { lineWidth: 8 });
  }
  if (hovering?.fen === nFen) shapes = shapes.concat(makeShapesFromUci(color, hovering.uci, 'paleBlue'));
  ctrl.fork.hover(hovering?.uci);

  if (ctrl.isCevalAllowed() && ctrl.showBestMoveArrows() && ctrl.showEvaluation()) {
    if (isUci(nEval.best)) shapes = shapes.concat(makeShapesFromUci(rcolor, nEval.best, 'paleGreen'));
    if (!hovering && ctrl.ceval.search.multiPv) {
      const bestPvMoves = nCeval ? nCeval.pvs[0]?.moves : undefined;
      const nextBest = bestPvMoves?.[0] || ctrl.nextNodeBest();

      // one entry per ranked arrow drawn below, ranked by index into nCeval.pvs so a
      // dropped line leaves a gap rather than renumbering the survivors
      const ranked: { rank: number; uci: Uci; lineWidth: number }[] = [];

      if (nextBest) {
        drawManeuver(ctrl, color, bestPvMoves || [nextBest], 'paleBlue', shapes);
        // first move only: with maneuver arrows on, drawManeuver draws successive moves
        // of the same pv, which are not ranks
        if (bestPvMoves) ranked.push({ rank: 1, uci: nextBest, lineWidth: CG_PALE_BLUE_LINE_WIDTH });
      }

      if (nCeval?.pvs[1] && !(ctrl.threatMode() && nThreat && nThreat.pvs.length > 2)) {
        nCeval.pvs.forEach(function (pv, i) {
          if (pv.moves[0] === nextBest) return;
          const shift = winningChances.povDiff(color, nCeval.pvs[0], pv);
          if (shift >= 0 && shift < 0.2) {
            const lineWidth = Math.round(12 - shift * 50); // 12 to 2
            shapes = shapes.concat(makeShapesFromUci(color, pv.moves[0], 'paleGrey', { lineWidth }));
            ranked.push({ rank: i + 1, uci: pv.moves[0], lineWidth });
          }
        });
      }

      // a lone "1" is noise. Judged on the arrows drawn, not on multiPv, which overcounts
      if (ctrl.settings.showArrowRanks && ranked.length > 1) {
        const flip = ctrl.bottomColor() === 'black';
        shapes = shapes.concat(ranked.map(r => rankShape(r.rank, r.uci, r.lineWidth, flip)).filter(defined));
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
  if (ctrl.showMoveAnnotations()) {
    const glyphs = [...(ctrl.node.glyphs ?? [])];
    const liveGlyph = ctrl.liveAnnotate?.get(ctrl.path);
    if (liveGlyph && ctrl.settings.showLiveAnnotations && !glyphs.some(g => g.id <= 6))
      glyphs.push(liveGlyph);
    shapes = shapes.concat(annotationShapes({ ...ctrl.node, glyphs }));
  }
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
      ctrl.motif.detectPins(board).forEach(p => addAnalysis(makeSquare(p.pinned), 'pin'));
      ctrl.motif
        .detectUndefended(board, epSquare)
        .forEach(u => addAnalysis(makeSquare(u.square), 'undefended'));
      ctrl.motif
        .detectCheckable(board, epSquare, castlingRights)
        .forEach(s => addAnalysis(makeSquare(s.king), 'checkable'));
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
    opacity: 0.5,
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
