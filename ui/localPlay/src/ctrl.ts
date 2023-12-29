import { LocalPlayOpts } from './interfaces';
import { type LibotCtrl } from 'libot';
//import { makeRound } from './data';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { Chess } from 'chessops';
import * as Chops from 'chessops';

export class Ctrl {
  libot: LibotCtrl;
  chess = Chess.default();
  tellRound: SocketSend;
  fiftyMovePly = 0;
  loaded: Promise<void>;
  threefoldFens: Map<string, number> = new Map();

  constructor(
    readonly opts: LocalPlayOpts,
    readonly redraw: () => void,
  ) {
    this.loaded = lichess.asset.loadEsm<LibotCtrl>('libot').then(libot => {
      this.libot = libot;
      this.libot.setBot('coral');
    });
  }

  reset(/*fen: string*/) {
    this.fiftyMovePly = 0;
    this.threefoldFens.clear();
    this.chess.reset();
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
    console.log('move', uci);
    const move = Chops.parseUci(uci);
    if (!move || !this.chess.isLegal(move)) throw new Error(`illegal move ${uci}, ${this.fen}}`);
    const san = makeSanAndPlay(this.chess, move);
    this.fifty(move);
    this.threefold('update');
    const { end, result, reason } = this.checkGameOver();
    if (end) this.doGameOver(result!, reason!);

    this.tellRound('move', { uci, san, fen: this.fen, ply: this.ply, dests: this.dests });
  }

  userMove(uci: Uci) {
    this.move(uci);
    this.botMove();
  }

  async botMove() {
    console.log('bot move');
    const uci = await this.libot!.move(this.fen);
    console.log('got bot move', uci);
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

  get fen() {
    return makeFen(this.chess.toSetup());
  }

  get ply() {
    return 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
  }
}
