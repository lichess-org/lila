import * as co from 'chessops';
import { rankBotMatchup } from './rankBot';
import * as u from './util';
import type { Automator, Result, Matchup, Outcome, Libot } from '../types';
import type { GameCtrl } from '../gameCtrl';

interface Test {
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
  flipped: boolean = false;
  script: Script;

  constructor(
    readonly gameCtrl: GameCtrl,
    readonly redraw: () => void,
  ) {
    site.pubsub.on('flip', (flipped: boolean) => {
      this.flipped = flipped;
      this.redraw();
    });
    site.pubsub.on('theme', this.redraw);
    gameCtrl.setAutomator(this);
    this.reset(false);
  }

  onReset(): void {
    const bottom = this.bottomColor;
    const top = co.opposite(bottom);
    this.gameCtrl.roundData.player.name = this[bottom]?.name ?? 'Player';
    this.gameCtrl.roundData.opponent.name = this[top]?.name ?? 'Player';
    this.gameCtrl.round.cg?.set({ orientation: this.bottomColor });
  }

  onMove(fen: string): void {}

  async run(test?: Test, iterations: number = 1): Promise<void> {
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

  stop(): void {
    if (this.stopped) return;
    console.log('stopping');
    this.stopped = true;
    this.botCtrl.stop();
    this.redraw();
  }

  onGameEnd(outcome: Outcome | 'error', reason: string): boolean {
    if (outcome === 'error') {
      console.error(
        `${this.white?.name ?? 'Player'} (white) vs ${
          this.black?.name ?? 'Player'
        } (black) - ${outcome} ${reason} - ${this.gameCtrl.fen} ${this.gameCtrl.game.moves.join(' ')}`,
        JSON.stringify(this.gameCtrl.chess),
      );
      return false;
    }
    const last = { outcome: outcome, reason, white: this.white?.uid, black: this.black?.uid };
    this.script.results.push(last);

    const whiteRating = { r: this.white?.glicko?.r ?? 1500, rd: this.white?.glicko?.rd ?? 350 };
    this.botCtrl.updateRating(this.white, u.score(outcome, 'white'), this.black?.glicko);
    this.botCtrl.updateRating(this.black, u.score(outcome, 'black'), whiteRating);

    if (this.script.type === 'rank')
      this.script.games.push(...rankBotMatchup(this.botCtrl.bot(this.script.players[0])!, last));

    this.stopped = !this.testInProgress;
    if (this.stopped) return false;

    const game = this.script.games[this.script.results.length];
    this.botCtrl.whiteUid = game.white;
    this.botCtrl.blackUid = game.black;
    this.gameCtrl.reset({ white: game.white, black: game.black, startingFen: this.startingFen });
    this.run();
    return true;
  }

  reset(full: boolean = true): void {
    // TODO: wtf fix this
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
  }

  get startingFen(): string {
    return this.gameCtrl.setup.fen ?? co.fen.INITIAL_FEN;
  }

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

  get isStopped(): boolean {
    return !!this.white && !!this.black && this.stopped;
  }

  get hasUser(): boolean {
    return !(this.white && this.black);
  }

  get testInProgress(): boolean {
    return this.script.results.length < this.script.games.length;
  }

  get gameInProgress(): boolean {
    return this.gameCtrl.roundData.steps.length > 1 && !this.gameCtrl.checkGameOver().end;
  }

  private get white(): Libot | undefined {
    return this.botCtrl.white;
  }

  private get black(): Libot | undefined {
    return this.botCtrl.black;
  }

  private get botCtrl() {
    return this.gameCtrl.botCtrl;
  }

  private matchups(test: Test, iterations = 1): Matchup[] {
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

  /*get scriptCopy() {
    return {
      ...this.script,
      players: this.script.players.slice(),
      games: this.script.games.slice(),
      results: this.script.results.slice(),
    };
  }*/
}
