import { Chess } from 'chessops/chess';
import { parseFen, makeFen } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { parseSquare, makeUci } from 'chessops/util';
import { uciCharPair } from 'chess';

export default function(opts) {

  function piotr(sq: number): string {
    if (sq < 26) return String.fromCharCode('a'.charCodeAt(0) + sq);
    else if (sq < 52) return String.fromCharCode('A'.charCodeAt(0) + sq - 26);
    else if (sq < 62) return String.fromCharCode('0'.charCodeAt(0) + sq - 52);
    else if (sq == 62) return '!';
    else return '?';
  }

  function makeDests(pos: Chess): string {
    const result: string[] = [];
    for (const [from, squares] of pos.allDests()) {
      if (squares.nonEmpty()) result.push([from, ...Array.from(squares)].map(piotr).join(''));
    }
    return result.join(' ');
  }

  function sendAnaMove(req) {
    const setup = parseFen(req.fen).unwrap();
    const pos = Chess.fromSetup(setup).unwrap();
    const uci = {
      from: parseSquare(req.orig)!,
      to: parseSquare(req.dest)!,
      promotion: req.promotion,
    };
    const san = makeSanAndPlay(pos, uci);
    setTimeout(() => opts.addNode({
      ply: 2 * (pos.fullmoves - 1) + (pos.turn == 'white' ? 0 : 1),
      fen: makeFen(pos.toSetup()),
      id: uciCharPair(uci),
      dests: makeDests(pos),
      children: [],
      san,
      uci: makeUci(uci),
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
