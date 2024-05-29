import * as co from 'chessops';
//import { ObjectStorage, objectStorage } from 'common/objectStorage';
import { PlayCtrl } from './playCtrl';
import { Automator, Libot, Result, Matchup } from './interfaces';
import { Database } from './database';
import { clamp } from 'common';
//import { Libot } from './interfaces';

export interface Test {
  type: 'matchup' | 'roundRobin' | 'rank';
  iterations: number;
  players: string[];
  time: string;
  startingFen?: string;
}

export interface Script extends Test {
  games: Matchup[];
  results: Result[];
}

export class TestCtrl implements Automator {
  stopped = true;
  flipped = false;
  startingFen = co.fen.INITIAL_FEN;
  script: Script = {
    type: 'matchup',
    iterations: 1,
    players: [],
    time: 'classical',
    games: [],
    results: [],
  };

  constructor(
    readonly root: PlayCtrl,
    readonly redraw: () => void,
  ) {
    root.setAutomator(this);
    site.pubsub.on('flip', (flipped: boolean) => {
      this.flipped = flipped;
      this.redraw();
    });
    if (root.setup.white && root.setup.black) {
      this.script.players = [root.setup.white, root.setup.black];
      this.script.games = this.matchups(this.script);
    }
  }

  get botCtrl() {
    return this.root.botCtrl;
  }

  get db(): Database {
    return this.root.db;
  }

  get bottomColor(): Color {
    const o = { top: this.black, bottom: this.white };
    const wi = this.script.players.indexOf(this.white?.uid);
    const bi = this.script.players.indexOf(this.black?.uid);
    if (bi < wi) [o.top, o.bottom] = [o.bottom, o.top];
    if (this.flipped) [o.top, o.bottom] = [o.bottom, o.top];
    return o.bottom === this.botCtrl.players.white ? 'white' : 'black';
  }

  onReset() {
    const bottom = this.bottomColor;
    const top = co.opposite(bottom);
    this.root.roundData.player = this.root.player(bottom, this.botCtrl.players[bottom]?.name ?? 'Player');
    this.root.roundData.opponent = this.root.player(top, this.botCtrl.players[top]?.name ?? 'Player');
    this.root.round.cg?.set({ orientation: this.bottomColor });
  }

  async play(test?: Test) {
    if (test) {
      this.script = { ...test, games: this.matchups(test), results: [] };
      this.botCtrl.setBot('white', this.script.games[0].white);
      this.botCtrl.setBot('black', this.script.games[0].black);
    }
    if (test || this.root.checkGameOver().end) this.root.resetBoard(this.startingFen);
    this.stopped = false;
    this.root.botMove();
    this.redraw();
  }

  stop() {
    this.stopped = true;
    this.botCtrl.stop();
    this.redraw();
  }

  get canPlay() {
    return !this.root.isUserTurn;
  }

  get isStopped() {
    return this.botCtrl.players.white && this.botCtrl.players.black && this.stopped;
  }

  get matchInProgress() {
    return this.script.results.length < this.script.games.length;
  }

  get gameInProgress() {
    return this.root.roundData.steps.length > 1 && !this.root.checkGameOver().end;
  }

  onGameEnd(result: string, reason: string) {
    if (false)
      console.log(
        `${this.white?.name ?? 'Player'} (white) vs ${
          this.black?.name ?? 'Player'
        } (black) - ${result} ${reason} - ${this.startingFen} ${this.root.moves.join(' ')}`,
      );
    this.script.results.push({
      result: result as Color | 'draw',
      reason,
      white: this.white?.uid,
      black: this.black?.uid,
    });
    if (this.script.results.length >= this.script.games.length) {
      if (this.script.type === 'rank') {
        const { matchup, rating } = this.botCtrl.rankNext(this.script.results);
        if (matchup) this.script.games.push(matchup);
        else this.stopped = true;
      } else this.stopped = true;
    }
    if (this.isStopped) return;
    const game = this.script.games[this.script.results.length];
    this.botCtrl.setBot('white', game.white);
    this.botCtrl.setBot('black', game.black);
    this.root.resetBoard(this.startingFen);
    this.play();
  }

  matchups(test: Test): Matchup[] {
    const players = test.players;
    const games: Matchup[] = [];
    if (test.type === 'rank') return [this.botCtrl.rankMatchup(15, this.botCtrl.getBot(players[0]))];
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

  get scriptCopy() {
    return {
      ...this.script,
      players: this.script.players.slice(),
      games: this.script.games.slice(),
      results: this.script.results.slice(),
    };
  }

  get white() {
    return this.botCtrl.players.white!;
  }

  get black() {
    return this.botCtrl.players.black!;
  }

  resultsText(color: Color) {
    const players = this.botCtrl.players;
    const player = players[color]?.uid;
    const winner = (r: Result, uid?: string) =>
      (r.result === 'white' || r.result === 'black') && r[r.result] === uid;
    const loser = (r: Result, uid?: string) =>
      (r.result === 'white' || r.result === 'black') && r[co.opposite(r.result)] === uid;
    const draw = (r: Result, uid?: string) => r.result === 'draw' && (r.white === uid || r.black === uid);
    const results = this.script.results;

    const wins = results.filter(r => winner(r, player)).length;
    const losses = results.filter(r => loser(r, player)).length;
    const draws = results.filter(r => draw(r, player)).length;
    return `${wins}W / ${draws}D / ${losses}L`;
  }

  swapColors() {
    /*this.totals = {
      white: this.totals.black,
      black: this.totals.white,
      draw: this.totals.draw,
      gamesLeft: this.totals.gamesLeft,
      error: this.totals.error,
    };
    this.botCtrl.swap();
    const cg = this.root.round.cg!;
    cg.set({ orientation: cg.state.orientation === 'white' ? 'black' : 'white' });
    this.root.reset(this.startingFen);*/
  }
}
