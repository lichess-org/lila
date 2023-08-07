import { LocalPlayOpts } from './interfaces';
import { makeRounds } from './data';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { Chess } from 'chessops';
import * as Chops from 'chessops';
import makeZerofish, { Zerofish, PV } from 'zerofish';

type Tab = string;
export class Ctrl {
  path = '';
  chess = Chess.default();
  zf: Zerofish | undefined;
  round: SocketSend;
  fen = '';
  fiftyMovePly = 0;
  threefoldFens: Map<string, number> = new Map();

  constructor(readonly opts: LocalPlayOpts, readonly redraw: () => void) {
    makeZerofish().then(zf => {
      this.zf = zf;
      // fetch model as arrraybuffer and
      //this.zf!.setZeroWeights(new Uint8Array());
    });
    makeRounds(this).then(round => (this.round = round));
  }

  checkGameOver(userEnd?: 'whiteResign' | 'blackResign' | 'mutualDraw'): {
    end: boolean;
    result?: string;
    reason?: string;
  } {
    let result = 'draw',
      reason = userEnd ?? 'checkmate';
    if (this.chess.isCheckmate()) result = Chops.opposite(this.chess.turn);
    else if (this.chess.isInsufficientMaterial()) reason = 'insufficient';
    else if (this.chess.isStalemate()) reason = 'stalemate';
    else if (this.fifty()) reason = 'fifty';
    else if (this.threefold()) reason = 'threefold';
    else if (userEnd) {
      if (userEnd !== 'mutualDraw') reason = 'resign';
      if (userEnd === 'whiteResign') result = 'black';
      else if (userEnd === 'blackResign') result = 'white';
    } else return { end: false };
    return { end: true, result, reason };
  }

  doGameOver(result: string, reason: string) {
    console.log(`game over ${result} ${reason}`);

    // blah blah do outcome stuff
  }

  move(uci: Uci) {
    const move = Chops.parseUci(uci);
    if (!move || !this.chess.isLegal(move))
      throw new Error(`illegal move ${uci}, ${makeFen(this.chess.toSetup())}}`);
    console.log(
      `before - turn ${this.chess.turn}, half ${this.chess.halfmoves}, full ${this.chess.fullmoves}, fen '${this.fen}'`
    );
    const san = makeSanAndPlay(this.chess, move);
    console.log(this.chess.fullmoves);
    this.fen = makeFen(this.chess.toSetup());
    console.log(
      `after - turn ${this.chess.turn}, half ${this.chess.halfmoves}, full ${this.chess.fullmoves}, fen '${this.fen}'`
    );
    this.fifty(move);
    this.threefold('update');
    const { end, result, reason } = this.checkGameOver();
    if (end) this.doGameOver(result!, reason!);

    this.round('move', {
      uci,
      fen: this.fen,
      ply: 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0),
      dests: this.dests,
      san,
    });
  }

  userMove(uci: Uci) {
    this.move(uci);
    this.getBotMove();
  }

  async getBotMove() {
    const uci = (await this.zf!.goFish(this.fen, { depth: 10 }))[0].moves[0];
    this.move(uci);
  }

  fifty(move?: Chops.Move) {
    if (move)
      if (
        !('from' in move) ||
        this.chess.board.getRole(move.from) === 'pawn' ||
        this.chess.board.get(move.to)
      )
        this.fiftyMovePly = 0;
      else this.fiftyMovePly++;
    return this.fiftyMovePly >= 100;
  }

  threefold(update: 'update' | false = false) {
    const boardFen = this.fen.split('-')[0];
    const fenCount = (this.threefoldFens.get(boardFen) ?? 0) + 1;
    if (update) this.threefoldFens.set(boardFen, fenCount);
    return fenCount >= 3;
  }

  isPromotion(move: Chops.Move) {
    return (
      'from' in move &&
      Chops.squareRank(move.to) === (this.chess.turn === 'white' ? 7 : 0) &&
      this.chess.board.getRole(move.from) === 'pawn'
    );
  }

  get dests() {
    const dests: { [from: string]: string } = {};
    [...this.chess.allDests()]
      .filter(([, to]) => !to.isEmpty())
      .forEach(([s, ds]) => (dests[Chops.makeSquare(s)] = [...ds].map(Chops.makeSquare).join('')));
    return dests;
  }
}

function linesWithin(move: string, lines: PV[], bias = 0, threshold = 50) {
  const zeroScore = lines.find(line => line.moves[0] === move)?.score ?? Number.NaN;
  return lines.filter(fish => Math.abs(fish.score - bias - zeroScore) < threshold && fish.moves.length);
}

function randomSprinkle(move: string, lines: PV[]) {
  lines = linesWithin(move, lines, 0, 20);
  if (!lines.length) return move;
  return lines[Math.floor(Math.random() * lines.length)].moves[0] ?? move;
}

/*
function occurs(chance: number) {
  return Math.random() < chance;
}*/
