import * as Chops from 'chessops';
//import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { PlayCtrl } from './playCtrl';
import { Automator } from './interfaces';
import { Database } from './database';
//import { Libot } from './interfaces';

export interface Result {
  winner: Color | undefined;
  reason: string;
}

export interface FullResult {
  winner: Color | undefined;
  white: string;
  black: string;
  result: string;
  reason: string;
  moves: number | string[];
}

export interface Test {
  type: 'matchup' | 'roundRobin' | 'rankBot';
  iterations: number;
  players: string[];
  nfold: number;
  time: string;
  startingFen?: string;
}

export interface Matchup {
  white: string;
  black: string;
}

export interface Script extends Test {
  games: Matchup[];
  results: Result[];
}

export class TestCtrl implements Automator {
  totals: { gamesLeft: number; white: number; black: number; draw: number; error: number };
  ids: { white: string; black: string };
  stopped = true;
  flipped = false;
  startingFen = Chops.fen.INITIAL_FEN;
  script: Script = {
    type: 'matchup',
    iterations: 1,
    players: [],
    nfold: 3,
    time: '5+0',
    games: [],
    results: [],
  };
  //scriptStore: ObjectStorage<Script>;
  newScript = true;

  constructor(
    readonly root: PlayCtrl,
    readonly redraw: () => void,
  ) {
    root.setAutomator(this);
    this.totals ??= { gamesLeft: 1, white: 0, black: 0, draw: 0, error: 0 };
    site.pubsub.on('flip', (flipped: boolean) => {
      this.flipped = flipped;
      this.redraw();
    });
    if (root.setup.white && root.setup.black) {
      this.script.players = [root.setup.white, root.setup.black];
      this.script.games = this.matchups(this.script);
    }
    /*objectStorage<Script>({ store: 'local.test.script' }).then(store => {
      this.scriptStore = store;
      store?.get('script').then(script => {
        if (script?.results?.length ?? 0 < script?.games?.length ?? 0) this.script = script;
      });
    });*/
  }

  get db(): Database {
    return this.root.db;
  }

  get bottomPlayer(): Color {
    const o = { top: this.black, bottom: this.white };
    const wi = this.script.players.indexOf(this.white?.uid);
    const bi = this.script.players.indexOf(this.black?.uid);
    if (bi < wi) [o.top, o.bottom] = [o.bottom, o.top];
    if (this.flipped) [o.top, o.bottom] = [o.bottom, o.top];
    return o.bottom === this.root.botCtrl.players.white ? 'white' : 'black';
  }

  onReset() {
    const bottom = this.bottomPlayer;
    const top = Chops.opposite(bottom);
    this.root.roundData.player = this.root.player(
      bottom,
      this.root.botCtrl.players[bottom]?.name ?? 'Player',
    );
    this.root.roundData.opponent = this.root.player(top, this.root.botCtrl.players[top]?.name ?? 'Player');
    //const cg = this.root.round.cg!;
    this.root.round.cg?.set({ orientation: this.bottomPlayer });

    console.log('on reset');
  }

  storeParams() {
    console.log('store ui params');
  }

  fetchParams() {
    console.log('fetch ui params');
  }

  async play(test?: Test) {
    if (test) {
      this.script = { ...test, games: this.matchups(test), results: [] };
      this.root.botCtrl.setBot('white', this.script.games[0].white);
      this.root.botCtrl.setBot('black', this.script.games[0].black);
    }
    if (test || this.root.checkGameOver().end) this.root.reset(this.startingFen);
    this.stopped = false;
    this.root.botMove();
    this.redraw();
  }

  async rank(uid: string) {
    const bot = this.root.botCtrl.bots[uid];
    if (bot) {
      const games: Matchup[] = [];
    }
  }

  stop() {
    this.stopped = true;
    this.root.botCtrl.stop();
    this.redraw();
  }

  get canPlay() {
    return !this.root.isUserTurn;
  }

  get isStopped() {
    return this.root.botCtrl.players.white && this.root.botCtrl.players.black && this.stopped;
  }

  get inProgress() {
    return this.script.results.length < this.script.games.length;
  }

  onGameStart(white: string, black: string) {
    this.ids = { white, black };
    this.totals.gamesLeft = this.script.iterations - this.script.results.length;
    this.redraw();
  }

  onGameEnd(result: string, reason: string) {
    console.log(
      `${this.white?.name ?? 'Player'} (white) vs ${
        this.black?.name ?? 'Player'
      } (black) - ${result} ${reason} - ${this.startingFen} ${this.root.moves.join(' ')}`,
    );
    this.script.results.push({ winner: result === 'draw' ? undefined : (result as Color), reason });
    //this.scriptStore?.put('script', this.scriptCopy);
    if (this.script.results.length >= this.script.games.length) {
      if (this.script.type === 'rankBot') {
        //
      } else this.stopped = true;
      return;
    }
    const game = this.script.games[this.script.results.length];
    this.root.botCtrl.setBot('white', game.white);
    this.root.botCtrl.setBot('black', game.black);

    /*if (this.script.type === 'matchup') {
      const cg = this.root.round.cg!;
      cg.set({ orientation: Chops.opposite(cg.state.orientation) });
    }*/

    if (!this.isStopped) {
      this.root.reset(this.startingFen);
      this.play();
    }
  }

  get scriptCopy() {
    return {
      ...this.script,
      players: this.script.players.slice(),
      games: this.script.games.slice(),
      results: this.script.results.slice(),
    };
  }

  get white() {
    return this.root.botCtrl.players.white!;
  }

  get black() {
    return this.root.botCtrl.players.black!;
  }

  resultsText(color: Color) {
    const wins = this.script.results.filter(r => r.winner === color).length;
    const draws = this.script.results.filter(r => !r.winner).length;
    const losses = this.script.results.length - wins - draws;
    return `${wins}W / ${draws}D / ${losses}L`;
  }

  matchups(test: Test): Matchup[] {
    const players = test.players;
    const games: Matchup[] = [];
    for (let it = 0; it < test.iterations; it++) {
      if (test.type === 'roundRobin') {
        for (let i = 0; i < players.length; i++) {
          for (let j = i + 1; j < players.length; j++) {
            games.push({ white: players[i], black: players[j] });
            games.push({ white: players[j], black: players[i] });
          }
        }
      } else games.push({ white: test.players[it % 2], black: test.players[(it + 1) % 2] });
    }
    return games;
  }

  swapColors() {
    /*this.totals = {
      white: this.totals.black,
      black: this.totals.white,
      draw: this.totals.draw,
      gamesLeft: this.totals.gamesLeft,
      error: this.totals.error,
    };
    this.root.botCtrl.swap();
    const cg = this.root.round.cg!;
    cg.set({ orientation: cg.state.orientation === 'white' ? 'black' : 'white' });
    this.root.reset(this.startingFen);*/
  }
}
