import { Chess } from 'chessops/chess';
import { parseSan } from 'chessops/san';
import type { Piece } from 'chessops/types';
import { kingCastlesTo, rookCastlesTo, roleToChar, squareFile, squareRank } from 'chessops/util';

import { pieceSrc } from './html';

export interface Board {
  el: HTMLElement;
  play(san: string): boolean;
}

const pieceUrl = (p: Piece) =>
  `url(${pieceSrc(p.color === 'white' ? 'w' : 'b', roleToChar(p.role).toUpperCase())})`;

export const parsePgnMoves = (pgn: string): string[] =>
  pgn
    .split(/\s+/)
    .filter(t => t && !/^\d+\.+$/.test(t))
    .map(t => t.replace(/^\d+\.+/, ''));

// a board of DOM pieces that slide between squares, so a few moves can be animated cheaply
export function makeBoard(orientation: Color = 'white'): Board {
  const pos = Chess.default();
  const wrap = document.createElement('div');
  wrap.className = 'board-wrap';
  const board = document.createElement('div');
  board.className = 'board';
  wrap.appendChild(board);
  const highlights = [0, 1].map(() => {
    const h = document.createElement('div');
    h.className = 'board__hl';
    board.appendChild(h);
    return h;
  });
  const at = (node: HTMLElement, sq: number) => {
    const f = squareFile(sq);
    const r = squareRank(sq);
    const [x, y] = orientation === 'white' ? [f, 7 - r] : [7 - f, r];
    node.style.transform = `translate(${x * 100}%, ${y * 100}%)`;
  };
  // square -> piece node, kept in sync with the position
  const nodes = new Map<number, HTMLElement>();
  for (const [sq, piece] of pos.board) {
    const n = document.createElement('div');
    n.className = 'board__piece';
    n.style.backgroundImage = pieceUrl(piece);
    at(n, sq);
    board.appendChild(n);
    nodes.set(sq, n);
  }

  const slide = (from: number, to: number) => {
    const n = nodes.get(from);
    if (!n) return;
    nodes.delete(from);
    nodes.set(to, n);
    n.classList.add('board__piece--lift');
    at(n, to);
    setTimeout(() => n.classList.remove('board__piece--lift'), 560);
  };
  const capture = (sq: number) => {
    const n = nodes.get(sq);
    if (!n) return;
    nodes.delete(sq);
    setTimeout(() => n.classList.add('board__piece--gone'), 300);
    setTimeout(() => n.remove(), 700);
  };

  const play = (san: string): boolean => {
    const move = parseSan(pos, san);
    if (!move || !('from' in move)) return false;
    const { from, to } = move;
    const piece = pos.board.get(from)!;
    const target = pos.board.get(to);
    let dest = to;
    if (piece.role === 'king' && target?.role === 'rook' && target.color === piece.color) {
      // chessops encodes castling as the king taking its own rook
      const side = to > from ? 'h' : 'a';
      dest = kingCastlesTo(piece.color, side);
      slide(from, dest);
      slide(to, rookCastlesTo(piece.color, side));
    } else {
      if (target) capture(to);
      else if (piece.role === 'pawn' && squareFile(from) !== squareFile(to))
        capture(to + (piece.color === 'white' ? -8 : 8)); // en passant
      slide(from, to);
    }
    pos.play(move);
    if (move.promotion) nodes.get(dest)!.style.backgroundImage = pieceUrl(pos.board.get(dest)!);
    [from, dest].forEach((sq, i) => {
      at(highlights[i], sq);
      highlights[i].style.opacity = '1';
    });
    return true;
  };

  return { el: wrap, play };
}
