import { makeFen } from 'chessops/fen';
import { Chess, Role } from 'chessops';
import * as Chops from 'chessops';
import { Api as CgApi } from 'chessground/api';
import { Config as CgConfig } from 'chessground/config';
import { Key } from 'chessground/types';
import { Engines, LegacyBot } from 'ceval';
import { storedStringProp, storedIntProp } from 'common/storage';
import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { LocalCtrl } from './localCtrl';
import { GameObserver } from './interfaces';

type GameResult = {
  result: string;
  reason: string;
  white: string;
  black: string;
  nfold?: number;
  startingFen?: string;
  moves: number | string[];
};

export type Test =
  | { matchup: { p1: string; p2?: string } }
  | { roundRobin: number; players?: string[] }
  | { simul: string; players?: string[] };
export interface TestParams {
  startingFen?: string;
  nfold?: number;
  time?: string;
  white?: string;
  black?: string;
  test?: Test;
  iterations?: number;
}
export class TestCtrl implements GameObserver {
  totals: { gamesLeft: number; white: number; black: number; draw: number; error: number };
  ids: { white: string; black: string };
  //fen = '';
  key = 0;
  flipped = false;
  store: ObjectStorage<GameResult>;
  params: TestParams = {
    startingFen: Chops.fen.INITIAL_BOARD_FEN,
    nfold: 3,
    time: '100',
    white: 'stockfish',
    black: 'stockfish',
    iterations: 1,
  };

  constructor(
    readonly root: LocalCtrl,
    readonly redraw: () => void,
  ) {
    objectStorage<GameResult>({ store: 'local.test.results' }).then(async x => {
      this.store = x;
      this.key = await this.store.count();
    });
    root.observer = this;
    this.totals ??= { gamesLeft: 1, white: 0, black: 0, draw: 0, error: 0 };
  }

  storeParams() {
    objectStorage<TestParams>({ store: 'local.test' }).then(async x => {
      x.put('params', this.params);
    });
    this.totals = { gamesLeft: this.totals?.gamesLeft ?? 1, white: 0, black: 0, draw: 0, error: 0 };
  }

  fetchParams() {
    objectStorage<TestParams>({ store: 'local.test' }).then(async x => {
      this.params = (await x.get('params')) ?? this.params;
    });
  }

  get startingFen() {
    return this.params.startingFen ?? Chops.fen.INITIAL_FEN;
  }

  swapSides() {
    this.root.botCtrl.swap();
    this.totals = {
      white: this.totals.black,
      black: this.totals.white,
      draw: this.totals.draw,
      gamesLeft: this.totals.gamesLeft,
      error: this.totals.error,
    };
  }

  async go(params?: TestParams) {
    if (params) this.params = { ...params };
    this.root.reset();
    const [nextKey] = await Promise.all([this.store.count()]);
    this.key = nextKey;
    if (this.params.iterations) this.totals.gamesLeft = this.params.iterations;
    this.root.reset(this.startingFen);
    this.root.botMove();
    $('#go').addClass('disabled');
    this.redraw();
  }

  async onGameEnd(result: string, reason: string) {
    this.store.put(`game ${String(this.key++).padStart(4, '0')}`, {
      white: this.white.name,
      black: this.black.name,
      ...this.params,
      result,
      reason,
      moves: this.root.moves,
    });
    console.log(
      `${this.white.name} (white) vs ${this.black.name} (black) - ${result} ${reason} - ${
        this.params.startingFen
      } ${this.root.moves.join(' ')}`,
    );

    this.totals[result as 'white' | 'black' | 'draw'] += 1;

    if (--this.totals.gamesLeft < 1) {
      $('#go').removeClass('disabled');
      this.redraw();
      return;
    }
    setTimeout(() => {
      this.swapSides();
      this.go({});
    });
  }

  get white() {
    return this.root.botCtrl.players.white!;
  }
  get black() {
    return this.root.botCtrl.players.black!;
  }
  resultsText(color: Color) {
    return this.totals
      ? `${this.totals[color]}W / ${this.totals.draw + this.totals.error}D / ${
          this.totals[Chops.opposite(color)]
        }L`
      : '0W / 0D / 0L';
  }
}
