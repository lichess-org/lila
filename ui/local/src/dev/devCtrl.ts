import * as co from 'chessops';
import { rateBotMatchup } from './rateBot';
import * as u from './util';
import type { Automator, Libot } from '../types';
import { statusOf } from 'game/status';
import { defined } from 'common';
import type { GameCtrl } from '../gameCtrl';
import type { GameStatus, MoveContext } from '../localGame';

interface Test {
  type: 'matchup' | 'roundRobin' | 'rate';
  players: string[];
  startingFen?: string;
}

export interface Result {
  winner: Color | undefined;
  white?: string;
  black?: string;
  //reason: string;
}

export interface Matchup {
  white: string;
  black: string;
}

export interface Script extends Test {
  games: Matchup[];
}

export class DevCtrl implements Automator {
  hurry: boolean; // skip animations, sounds, and artificial move wait times (clock is still adjusted)
  sandbox: boolean; // you can move anyone's pieces in sandbox mode, but premoves are disabled
  script: Script;
  log: Result[];

  constructor(
    readonly gameCtrl: GameCtrl,
    readonly redraw: () => void,
  ) {
    site.pubsub.on('theme', this.redraw);
    gameCtrl.dev = this;
    const skip = localStorage.getItem('local.dev.hurry');
    this.hurry = skip ? skip === '1' : false;
    this.resetScript();
  }

  run(test?: Test, iterations: number = 1): boolean {
    if (test) {
      this.resetScript(test);
      this.script.games.push(...this.matchups(test, iterations));
    }
    const game = this.script.games.shift();
    if (!game) return false;
    this.gameCtrl.reset({ ...game, fen: this.startingFen });
    this.gameCtrl.start();
    this.redraw();
    return true;
  }

  resetScript(test?: Test): void {
    this.log ??= [];
    const players = [this.gameCtrl.setup.white, this.gameCtrl.setup.black].filter(x =>
      defined(x),
    ) as string[];
    this.script = {
      type: 'matchup',
      players,
      games: [],
      ...test,
    };
  }

  onReset(): void {}

  skip(movetime: number): boolean {
    if (!this.hurry) return false;
    if (this.gameCtrl.clock) this.gameCtrl.clock[this.gameCtrl.turn] -= movetime;
    return true;
  }

  preMove(moveResult: MoveContext): void {
    this.gameCtrl.round.chessground?.set({ animation: { enabled: !this.hurry } });
    if (this.hurry) moveResult.silent = true;
  }

  onGameOver({ winner, turn, reason, status }: GameStatus): boolean {
    console.log('devCtrl', winner, reason, status);
    const last = { winner, white: this.white?.uid, black: this.black?.uid };
    this.log.push(last);
    if (status === statusOf('cheat')) {
      console.error(
        `${this.white?.name ?? 'Player'} (white) vs ${
          this.black?.name ?? 'Player'
        } (black) - ${turn} ${reason} - ${this.gameCtrl.fen} ${this.gameCtrl.live.moves.join(' ')}`,
        JSON.stringify(this.gameCtrl.chess),
      );
      return false;
    }

    const whiteRating = { r: this.white?.glicko?.r ?? 1500, rd: this.white?.glicko?.rd ?? 350 };
    this.botCtrl.updateRating(this.white, u.score(winner, 'white'), this.black?.glicko);
    this.botCtrl.updateRating(this.black, u.score(winner, 'black'), whiteRating);

    if (this.script.type === 'rate')
      this.script.games.push(...rateBotMatchup(this.botCtrl.bot(this.script.players[0])!, last));

    if (this.testInProgress) return this.run();
    this.resetScript();
    this.redraw();
    return false;
  }

  get startingFen(): string {
    return this.gameCtrl.setup.fen ?? co.fen.INITIAL_FEN;
  }

  get hasUser(): boolean {
    return !(this.white && this.black);
  }

  get testInProgress(): boolean {
    return this.script.games.length !== 0;
  }

  get gameInProgress(): boolean {
    return this.gameCtrl.data.steps.length > 1 && !this.gameCtrl.status.end;
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
    if (test.type === 'rate') return rateBotMatchup(this.botCtrl.bot(players[0])!);
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
}
