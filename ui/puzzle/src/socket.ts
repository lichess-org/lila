import { Chess } from 'chessops/chess';
import { parseFen, makeFen } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { makeSquare, parseSquare, makeUci, parseUci } from 'chessops/util';
import { altCastles, uciCharPair } from 'chess';
import { defined } from 'common';

export default function(opts) {

  function piotr(sq: number): string {
    if (sq < 26) return String.fromCharCode('a'.charCodeAt(0) + sq);
    else if (sq < 52) return String.fromCharCode('A'.charCodeAt(0) + sq - 26);
    else if (sq < 62) return String.fromCharCode('0'.charCodeAt(0) + sq - 52);
    else if (sq == 62) return '!';
    else return '?';
  }

  function makeDests(pos: Chess): string {
    const dests = pos.allDests();

    // add two step castling moves (standard chess)
    const king = pos.board.kingOf(pos.turn);
    if (defined(king) && king & 4 && dests.has(king)) {
      if (dests.get(king)!.has(0)) dests.set(king, dests.get(king)!.with(2));
      if (dests.get(king)!.has(7)) dests.set(king, dests.get(king)!.with(6));
      if (dests.get(king)!.has(56)) dests.set(king, dests.get(king)!.with(58));
      if (dests.get(king)!.has(63)) dests.set(king, dests.get(king)!.with(62));
    }

    const result: string[] = [];
    for (const [from, squares] of dests) {
      if (squares.nonEmpty()) result.push([from, ...Array.from(squares)].map(piotr).join(''));
    }
    return result.join(' ');
  }

  function sendAnaMove(req) {
    const setup = parseFen(req.fen).unwrap();
    const pos = Chess.fromSetup(setup).unwrap();
    const move = {
      from: parseSquare(req.orig)!,
      to: parseSquare(req.dest)!,
      promotion: req.promotion,
    };
    const san = makeSanAndPlay(pos, move);
    const uci = san.startsWith('O-O') && altCastles[makeUci(move)] || makeUci(move);
    const check = pos.isCheck() ? pos.board.kingOf(pos.turn) : undefined;
    setTimeout(() => opts.addNode({
      ply: 2 * (pos.fullmoves - 1) + (pos.turn == 'white' ? 0 : 1),
      fen: makeFen(pos.toSetup()),
      id: uciCharPair(parseUci(uci)!),
      uci,
      san,
      dests: makeDests(pos),
      children: [],
      check: check ? makeSquare(check) : undefined,
    }, req.path), 10);
  }

  function sendAnaDests(req) {
    const setup = parseFen(req.fen).unwrap();
    const pos = Chess.fromSetup(setup).unwrap();
    setTimeout(() => opts.addDests(makeDests(pos), req.path), 10);
  }

  return {
    sendAnaMove: sendAnaMove,
    sendAnaDests: sendAnaDests,
  };
}
