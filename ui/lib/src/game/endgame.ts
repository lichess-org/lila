import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Outcome } from 'chessops';
import { parseFen } from 'chessops/fen';
import { makeSquare, opposite } from 'chessops/util';

import { fenColor } from './chess';
import { endgameGlyphs, type EndgameGlyph } from './glyphs';
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
  gameWinner: Color | undefined,
  gameStatus: StatusName,
): EndgameResult => {
  const outcome = node.outcome();
  const isTerminal = node.dests().size === 0;
  const isMate = isTerminal && node.check();
  const isStalemate = isTerminal && !node.check() && outcome && !outcome.winner;

  return {
    winner: isMate ? opposite(fenColor(node.fen)) : isStalemate ? undefined : (outcome?.winner ?? gameWinner),
    status: isMate ? 'mate' : isStalemate ? 'stalemate' : gameStatus,
  };
};

export function endgameShapesForNode(
  node: EndgameNode,
  gameWinner: Color | undefined,
  gameStatus: StatusName,
): DrawShape[] {
  const result = endgameResult(node, gameWinner, gameStatus);
  return endgameShapes(node.fen, result.winner, result.status);
}

const glyphRoles: Role[] = ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'];

export function findGlyphSquare(fen: FEN, color: Color): Key | undefined {
  const square = parseFen(fen).unwrap(
    setup => glyphRoles.map(role => setup.board.pieces(color, role).first()).find(sq => sq !== undefined),
    _ => undefined,
  );
  return square === undefined ? undefined : makeSquare(square);
}

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
    const square = findGlyphSquare(fen, color);
    if (square)
      shapes.push({
        orig: square,
        brush: '',
        customSvg: { html: endgameGlyphs[glyph](0), center: 'orig' },
      });
  };

  if (winner) {
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
