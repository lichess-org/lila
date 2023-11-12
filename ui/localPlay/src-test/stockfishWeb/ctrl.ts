import { BvbOpts, CgHost } from '../interfaces';
import { makeFen } from 'chessops/fen';
import { Chess, Role } from 'chessops';
import * as Chops from 'chessops';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';
import { Engines, LegacyBot } from 'ceval';
import { storedStringProp, storedIntProp } from 'common/storage';
import { ObjectStorage, objectStorage } from 'common/objectStorage';

export interface MatchParams {
  startingFen: string;
  movetime: number;
  threads: number;
  hash: number;
  nfold: number;
}

type GameResult = {
  result: string;
  reason: string;
  white: string;
  black: string;
  movetime: number;
  threads: number;
  hash: number;
  nfold?: number;
  startingFen: string;
  moves: number | string[];
};

export class StockfishWebCtrl implements CgHost {
  cg: CgApi;
  path = '';
  chess = Chess.default();
  bots: { white: LegacyBot; black: LegacyBot };
  totals: { gamesLeft: number; white: number; black: number; draw: number; error: number };
  ids: { white: string; black: string };
  params: MatchParams;
  engines = new Engines();
  fen = '';
  key = 0;
  flipped = false;
  fiftyMovePly = 0;
  threefoldFens: Map<string, number> = new Map();
  moves: string[] = [];
  results: ObjectStorage<GameResult>;

  constructor(
    readonly opts: BvbOpts,
    readonly redraw: () => void,
  ) {
    this.fetchParams();
    objectStorage<GameResult>({ store: 'bvb' }).then(async x => {
      this.results = x;
      this.key = await this.results.count();
    });
    this.totals ??= { gamesLeft: 1, white: 0, black: 0, draw: 0, error: 0 };
    this.bots = {
      white: this.engines.makeBot(this.ids.white),
      black: this.engines.makeBot(this.ids.black),
    };
  }

  fetchParams() {
    if (!this.ids)
      this.ids = {
        white: storedStringProp('one', '__sf16nnue7')(),
        black: storedStringProp('two', '__sf16nnue12')(),
      };
    this.params = {
      startingFen: storedStringProp('fen', '')(),
      nfold: storedIntProp('nfold', 12)(),
      movetime: storedIntProp('movetime', 100)(),
      threads: storedIntProp('threads', 4)(),
      hash: storedIntProp('hash', 16)(),
    };
  }

  storeParams() {
    storedIntProp('movetime', 100)(this.params.movetime);
    storedIntProp('threads', 4)(this.params.threads);
    storedIntProp('hash', 16)(this.params.hash);
    storedStringProp('startingfen', '')(this.params.startingFen);
    storedStringProp('one', '__sf16nnue7')(this.ids.white);
    storedStringProp('two', '__sf16nnue12')(this.ids.black);
    storedIntProp('nfold', 12)(this.params.nfold);
    this.totals = { gamesLeft: this.totals?.gamesLeft ?? 1, white: 0, black: 0, draw: 0, error: 0 };
  }

  swapSides() {
    this.bots = { white: this.bots.black, black: this.bots.white };
    this.ids = { white: this.ids.black, black: this.ids.white };
    this.totals = {
      white: this.totals.black,
      black: this.totals.white,
      draw: this.totals.draw,
      gamesLeft: this.totals.gamesLeft,
      error: this.totals.error,
    };
  }

  async go(numGames?: number) {
    this.moves = [];
    if (this.bots.white?.info.id !== this.ids.white) {
      this.bots.white = this.engines.makeBot(this.ids.white);
    }
    if (this.bots.black?.info.id !== this.ids.black) {
      this.bots.black = this.engines.makeBot(this.ids.black);
    }
    const [nextKey] = await Promise.all([
      this.results.count(),
      this.bots.white.load(),
      this.bots.black.load(),
    ]);
    this.key = nextKey;
    if (numGames) this.totals.gamesLeft = numGames;
    this.fiftyMovePly = 0;
    this.threefoldFens.clear();
    if (this.params.startingFen)
      this.chess = Chess.fromSetup(Chops.fen.parseFen(this.params.startingFen).unwrap()).unwrap();
    else this.chess.reset();
    this.fen = makeFen(this.chess.toSetup());
    this.cg.set({ fen: this.fen });
    this.bots.white?.reset(this.params);
    this.bots.black?.reset(this.params);
    this.getBotMove();
    $('#go').addClass('disabled');
    this.redraw();
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

  async doGameOver(result: string, reason: string) {
    this.results.put(`game ${String(this.key++).padStart(4, '0')}`, {
      white: this.bots.white.info.name,
      black: this.bots.black.info.name,
      ...this.params,
      result,
      reason,
      moves: this.moves,
    });
    console.log(
      `${this.bots.white.info.name} (white) vs ${this.bots.black.info.name} (black) - ${result} ${reason} - ${
        this.params.startingFen
      } ${this.moves.join(' ')}`,
    );

    this.totals[result as 'white' | 'black' | 'draw'] += 1;

    if (--this.totals.gamesLeft < 1) {
      $('#go').removeClass('disabled');
      this.redraw();
      return;
    }
    setTimeout(() => {
      this.swapSides();
      this.go();
    });
  }

  resultsText(color: Color) {
    return this.totals
      ? `${this.totals[color]}W / ${this.totals.draw + this.totals.error}D / ${
          this.totals[Chops.opposite(color)]
        }L`
      : '0W / 0D / 0L';
  }

  move(uci: Uci) {
    const move = Chops.parseUci(uci);
    this.moves.push(uci);
    if (!move || !this.chess.isLegal(move)) {
      this.doGameOver(
        'error',
        `${this.bots[this.chess.turn].info.name} made illegal move ${uci} at ${makeFen(
          this.chess.toSetup(),
        )}`,
      );
      return;
    }
    this.chess.play(move);
    this.fen = makeFen(this.chess.toSetup());
    this.fifty(move);
    this.threefold('update');
    this.updateCgBoard(uci);
    const { end, result, reason } = this.checkGameOver();
    if (end) this.doGameOver(result!, reason!);
    else this.getBotMove();
  }

  cgUserMove = (orig: Key, dest: Key) => {
    this.move(orig + dest);
  };

  async getBotMove(byFen = true) {
    const bot = this.bots[this.chess.turn]!;
    this.move(await bot.getMove(byFen ? this.fen : this.moves));
  }

  updateCgBoard(uci: Uci) {
    const { from, to, role } = splitUci(uci);
    this.cg.move(from, to);
    if (role) this.cg.setPieces(new Map([[to, { color: this.chess.turn, role, promoted: true }]]));
    this.cg.set(this.cgOpts(true));
  }

  cgOpts(withFen = true): CgConfig {
    return {
      fen: withFen ? this.fen : undefined,
      orientation: this.flipped ? 'black' : 'white',
      turnColor: this.chess.turn,
      check: this.chess.isCheck() ? this.chess.turn : false,
      movable: {
        color: this.chess.turn,
        dests: new Map(),
      },
    };
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
    let fenCount = this.threefoldFens.get(boardFen) ?? 0;
    if (update) this.threefoldFens.set(boardFen, ++fenCount);
    return this.params.nfold !== 0 ? fenCount >= this.params.nfold : false;
  }
}

function splitUci(uci: Uci): { from: Key; to: Key; role?: Role } {
  return { from: uci.slice(0, 2) as Key, to: uci.slice(2, 4) as Key, role: Chops.charToRole(uci.slice(4)) };
}
