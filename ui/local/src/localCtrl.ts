import { LocalPlayOpts, LocalSetup, GameObserver } from './interfaces';
import { Libot } from './bots/interfaces';
import { type BotCtrl } from './bots/botCtrl';
import { LocalDialog } from './setupDialog';
import { makeSocket } from './socket';
import { objectStorage } from 'common/objectStorage';
//import { makeRound } from './data';
import { makeFen /*, parseFen*/ } from 'chessops/fen';
import { makeSanAndPlay } from 'chessops/san';
import { MoveRootCtrl } from 'game';
import { RoundSocket, RoundOpts, RoundData } from 'round';
import { Chess } from 'chessops';
import * as Chops from 'chessops';

export class LocalCtrl {
  chess = Chess.default();
  socket: RoundSocket;
  fiftyMovePly = 0;
  threefoldFens: Map<string, number> = new Map();
  round: MoveRootCtrl;
  i18n: { [key: string]: string };
  setup: LocalSetup;
  moves: Uci[] = [];
  key = 0;
  roundData: RoundData;
  observer?: GameObserver;

  constructor(
    readonly opts: LocalPlayOpts,
    readonly botCtrl: BotCtrl,
    readonly redraw: () => void,
  ) {
    this.socket = makeSocket(this);
    this.setup = { fen: Chops.fen.INITIAL_FEN, ...opts.setup };
    this.i18n = opts.i18n;
    this.roundData = this.makeRoundData(this.setup.fen);
  }

  reset(fen?: string) {
    this.fiftyMovePly = 0;
    this.threefoldFens.clear();
    this.chess = Chess.fromSetup(Chops.fen.parseFen(fen ?? this.fen).unwrap()).unwrap();
    this.round.cg?.set({
      fen: fen ?? this.fen,
      turnColor: this.chess.turn,
      lastMove: undefined,
      movable: { color: this.chess.turn, dests: this.cgDests },
    });
    this.roundData = this.makeRoundData(fen);
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
    this.observer?.onGameEnd(result as 'white' | 'black' | 'draw', reason);
    this.reset();
    // blah blah do outcome stuff
  }

  move(uci: Uci): boolean {
    const move = Chops.parseUci(uci) as Chops.NormalMove;
    this.moves.push(uci);
    if (!move || !this.chess.isLegal(move)) {
      this.doGameOver(
        'error',
        `${this.botCtrl.players[this.chess.turn]?.name} made illegal move ${uci} at ${makeFen(
          this.chess.toSetup(),
        )}`,
      );
      return false;
    }
    const san = makeSanAndPlay(this.chess, move);
    this.fifty(move);
    this.threefold('update');
    this.socket.receive('move', { uci, san, fen: this.fen, ply: this.ply, dests: this.dests });
    this.updateRoundData();
    if (move.promotion)
      this.round.cg?.setPieces(
        new Map([
          [
            uci.slice(2, 4) as Cg.Key,
            { color: Chops.opposite(this.chess.turn), role: move.promotion, promoted: true },
          ],
        ]),
      );
    const { end, result, reason } = this.checkGameOver();
    if (end) {
      this.doGameOver(result!, reason!);
      this.redraw();
      return false;
    }
    this.redraw();
    if (this.botCtrl.players[this.chess.turn]) this.botMove();
    return true;
  }

  botMove = async () => {
    this.move(await this.botCtrl.move(this.fen, this.chess.turn));
  };

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
    let fenCount = this.threefoldFens.get(boardFen) ?? 0;
    if (update) this.threefoldFens.set(boardFen, ++fenCount);
    return fenCount >= (this.setup.nfold !== undefined ? this.setup.nfold : 3); // TODO fixme
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
  get cgDests() {
    const dec = new Map();
    const dests = this.dests;
    if (!dests) return dec;
    for (const k in dests) dec.set(k, dests[k].match(/.{2}/g) as Cg.Key[]);
    return dec;
  }

  get fen() {
    return makeFen(this.chess.toSetup());
  }

  get ply() {
    return 2 * (this.chess.fullmoves - 1) + (this.chess.turn === 'black' ? 1 : 0);
  }

  player(color: Color, name: string): RoundData['player'] {
    return {
      color,
      user: {
        id: name.toLowerCase().replace(' ', ''),
        username: name,
        online: true,
        perfs: {},
      },
      id: '',
      isGone: false,
      name,
      onGame: true,
      version: 0,
    };
  }

  get game(): RoundData['game'] {
    return {
      id: 'synthetic',
      variant: { key: 'standard', name: 'Standard', short: 'Std' },
      speed: 'classical',
      perf: 'classical',
      rated: false,
      fen: this.fen,
      turns: this.ply,
      source: 'local',
      status: { id: 20, name: 'started' },
      player: 'white',
    };
  }
  updateRoundData() {
    this.roundData.player = this.player(
      this.chess.turn,
      this.botCtrl.players[this.chess.turn]?.name ?? 'You',
    );
    this.roundData.opponent = this.player(
      Chops.opposite(this.chess.turn),
      this.botCtrl.players[Chops.opposite(this.chess.turn)]?.name ?? 'You',
    );
    this.roundData.game.player = this.chess.turn;
    this.round.cg?.set({ movable: { color: this.chess.turn, dests: this.cgDests } });
  }

  makeRoundData(fen?: string): RoundData {
    const opponent: 'black' | 'white' = this.chess.turn === 'white' ? 'black' : 'white';
    return {
      game: this.game,
      player: this.player(this.chess.turn, this.botCtrl.players[this.chess.turn]?.name ?? 'You'),
      opponent: this.player(opponent, this.botCtrl.players[opponent]?.name ?? 'You'),
      pref: this.opts.pref,
      steps: [{ ply: 0, san: '', uci: '', fen: fen ?? Chops.fen.INITIAL_FEN }],
      takebackable: true,
      moretimeable: true,
      possibleMoves: this.dests,
    };
  }
  get roundOpts(): RoundOpts {
    return {
      data: this.roundData,
      i18n: this.opts.i18n,
      local: this.socket,
      onChange: (d: RoundData) => {}, //console.log(d),
    };
  }
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: any } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: Chops.charToRole(uci.slice(4)) };
}
