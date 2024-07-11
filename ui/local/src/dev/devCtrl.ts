import * as co from 'chessops';
import { rankBotMatchup } from './rankBot';
import * as u from './util';
import type { Automator, Result, Matchup, Outcome } from '../types';
import type { GameCtrl } from '../gameCtrl';

export interface Test {
  type: 'matchup' | 'roundRobin' | 'rank';
  players: string[];
  time: string;
  startingFen?: string;
}

export interface Script extends Test {
  games: Matchup[];
  results: Result[];
}

export class DevCtrl implements Automator {
  private stopped = true;
  flipped = false;
  script: Script;

  constructor(
    readonly gameCtrl: GameCtrl,
    readonly redraw: () => void,
  ) {
    site.pubsub.on('flip', (flipped: boolean) => {
      this.flipped = flipped;
      this.redraw();
    });

    gameCtrl.setAutomator(this);
    this.reset(false);
  }

  get startingFen() {
    return this.gameCtrl.setup.fen ?? co.fen.INITIAL_FEN;
  }

  set startingFen(fen: string) {
    this.gameCtrl.setup.fen = fen;
  }

  get botCtrl() {
    return this.gameCtrl.botCtrl;
  }

  /*get db(): Database {
    return this.gameCtrl.db;
  }*/

  get bottomColor(): Color {
    if (!this.white) return 'white';
    if (!this.black) return 'black';
    const o = { top: this.black, bottom: this.white };
    const wi = this.script.players.indexOf(this.white?.uid);
    const bi = this.script.players.indexOf(this.black?.uid);
    if (bi < wi) [o.top, o.bottom] = [o.bottom, o.top];
    if (this.flipped) [o.top, o.bottom] = [o.bottom, o.top];
    return o.bottom === this.white ? 'white' : 'black';
  }

  onReset() {
    const bottom = this.bottomColor;
    const top = co.opposite(bottom);
    this.gameCtrl.roundData.player = this.gameCtrl.player(bottom, this[bottom]?.name ?? 'Player');
    this.gameCtrl.roundData.opponent = this.gameCtrl.player(top, this[top]?.name ?? 'Player');
    this.gameCtrl.round.cg?.set({ orientation: this.bottomColor });
  }

  onMove(fen: string) {}

  async run(test?: Test, iterations = 1) {
    if (test) {
      const index = this.script.results.length;
      this.script = { ...this.script, ...test };
      this.script.games = this.script.games.slice(0, index);
      this.script.games.push(...this.matchups(test, iterations));
      if (this.script.games.length > index) {
        this.botCtrl.whiteUid = this.script.games[index].white;
        this.botCtrl.blackUid = this.script.games[index].black;
      }
    }
    if (test || this.gameCtrl.checkGameOver().end) this.gameCtrl.resetBoard();
    this.stopped = false;
    this.gameCtrl.botMove();
    this.redraw();
  }

  stop() {
    if (this.stopped) return;
    this.stopped = true;
    this.botCtrl.stop();
    this.redraw();
  }

  get isStopped() {
    return this.white && this.black && this.stopped;
  }

  get hasUser() {
    return !(this.white && this.black);
  }

  get testInProgress() {
    return this.script.results.length < this.script.games.length;
  }

  get gameInProgress() {
    return this.gameCtrl.roundData.steps.length > 1 && !this.gameCtrl.checkGameOver().end;
  }

  onGameEnd(outcome: Outcome | 'error', reason: string): boolean {
    if (outcome === 'error') {
      console.error(
        `${this.white?.name ?? 'Player'} (white) vs ${
          this.black?.name ?? 'Player'
        } (black) - ${outcome} ${reason} - ${this.gameCtrl.fen} ${this.gameCtrl.moves.join(' ')}`,
        JSON.stringify(this.gameCtrl.chess),
      );
      return false;
    }
    const last = { outcome: outcome, reason, white: this.white?.uid, black: this.black?.uid };
    this.script.results.push(last);

    const whiteRating = { r: this.white?.glicko?.r ?? 1500, rd: this.white?.glicko?.rd ?? 350 };
    this.botCtrl.updateRating(this.white, this.black?.glicko, u.score(outcome, 'white'));
    this.botCtrl.updateRating(this.black, whiteRating, u.score(outcome, 'black'));

    if (this.script.type === 'rank')
      this.script.games.push(...rankBotMatchup(this.botCtrl.bot(this.script.players[0])!, last));

    this.stopped = !this.testInProgress;
    if (this.stopped) return false;

    const game = this.script.games[this.script.results.length];
    this.botCtrl.whiteUid = game.white;
    this.botCtrl.blackUid = game.black;
    this.gameCtrl.reset({ white: game.white, black: game.black, startingFen: this.startingFen, moves: [] });
    this.run();
    return true;
  }

  matchups(test: Test, iterations = 1): Matchup[] {
    const players = test.players;
    if (players.length < 2) return [];
    if (test.type === 'rank') return rankBotMatchup(this.botCtrl.bot(players[0])!);
    const games: Matchup[] = [];
    for (let it = 0; it < iterations; it++) {
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

  reset = (full = true) => {
    if (full) {
      this.stop();
      this.gameCtrl.resetToSetup();
    }
    this.script = {
      type: 'matchup',
      players: [],
      time: 'classical',
      games: [],
      results: [],
    };
    if (this.gameCtrl.setup.white && this.gameCtrl.setup.black) {
      this.script.players = [this.gameCtrl.setup.white, this.gameCtrl.setup.black];
      //this.script.games = this.matchups(this.script);
    }
    if (full) this.gameCtrl.redraw();
  };

  get scriptCopy() {
    return {
      ...this.script,
      players: this.script.players.slice(),
      games: this.script.games.slice(),
      results: this.script.results.slice(),
    };
  }

  get white() {
    return this.botCtrl.white!;
  }

  get black() {
    return this.botCtrl.black!;
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
    const cg = this.gameCtrl.round.cg!;
    cg.set({ orientation: cg.state.orientation === 'white' ? 'black' : 'white' });
    this.gameCtrl.reset(this.startingFen);*/
  }
}
