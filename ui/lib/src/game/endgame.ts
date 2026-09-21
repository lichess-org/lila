import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Outcome } from 'chessops';
import { parseFen } from 'chessops/fen';
import { makeSquare, opposite } from 'chessops/util';

import { fenColor } from './chess';
import { endgameGlyphs } from './glyphs';
import type { StatusName } from './status';

export interface EndgameResult {
  winner?: Color;
  status?: StatusName;
}

type EndgameNode = {
  fen: FEN;
  outcome: () => Outcome | undefined;
  check: () => boolean;
  dests: () => Dests;
};

const endgameResult = (
  node: EndgameNode,
  isLast: boolean,
  gameWinner: Color | undefined,
  gameStatus: StatusName,
): EndgameResult => {
  const isTerminal = node.dests().size === 0,
    isMate = node.check() && isTerminal,
    isStalemate = !node.check() && isTerminal,
    isGameEnd = isLast || isTerminal;

  if (!isGameEnd) return {};
  return {
    winner: isMate
      ? opposite(fenColor(node.fen))
      : isStalemate
        ? undefined
        : (node.outcome()?.winner ?? gameWinner),
    status: isMate ? 'mate' : isStalemate ? 'stalemate' : gameStatus,
  };
};

export function endgameShapesForNode(
  node: EndgameNode,
  isLast: boolean,
  gameWinner: Color | undefined,
  gameStatus: StatusName,
): DrawShape[] {
  const result = endgameResult(node, isLast, gameWinner, gameStatus);
  return endgameShapes(node.fen, result.winner, result.status);
}

export function findKingSquare(fen: FEN, color: Color): Key | undefined {
  const king = parseFen(fen).unwrap(
    setup => setup.board.kingOf(color),
    _ => undefined,
  );
  return king === undefined ? undefined : makeSquare(king);
}

type EndgameGlyph = keyof typeof endgameGlyphs;

const loserGlyph = (status: StatusName | undefined): EndgameGlyph =>
  status === 'mate'
    ? 'mate'
    : status === 'resign'
      ? 'resign'
      : status === 'timeout'
        ? 'abandoned'
        : status === 'outoftime'
          ? 'outoftime'
          : 'unknown';

const drawGlyph = (status: StatusName | undefined): EndgameGlyph | undefined =>
  status === 'stalemate'
    ? 'stalemate'
    : status === 'draw' ||
        status === 'insufficientMaterialClaim' ||
        status === 'outoftime' ||
        status === 'timeout'
      ? 'draw'
      : undefined;

export function endgameShapes(
  fen: FEN,
  winner: Color | undefined,
  status: StatusName | undefined,
): DrawShape[] {
  if (status === 'aborted' || status === 'noStart') return [];

  const shapes: DrawShape[] = [];
  const add = (color: Color, glyph: EndgameGlyph) => {
    const king = findKingSquare(fen, color);
    if (king)
      shapes.push({
        orig: king,
        brush: '',
        customSvg: { html: endgameGlyphs[glyph](0), center: 'orig' },
      });
  };

  if (winner) {
    add(winner, 'win');
    add(opposite(winner), loserGlyph(status));
  } else {
    const glyph = drawGlyph(status);
    if (glyph) {
      add('white', glyph);
      add('black', glyph);
    }
  }
  return shapes;
}
