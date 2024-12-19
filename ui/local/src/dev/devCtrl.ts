import { RateBot, rateBotMatchup } from './rateBot';
import type { BotInfo, LocalSpeed } from '../types';
import { statusOf } from 'game/status';
import { defined, Prop } from 'common';
import { shuffle } from 'common/algo';
import { type ObjectStorage, objectStorage } from 'common/objectStorage';
import { storedBooleanProp } from 'common/storage';
import type { GameStatus, MoveContext } from '../localGame';
import { env } from '../localEnv';
import stringify from 'json-stringify-pretty-compact';
import { pubsub } from 'common/pubsub';

export interface Result {
  winner: Color | undefined;
  white?: string;
  black?: string;
}

interface Test {
  type: 'matchup' | 'roundRobin' | 'rate';
  players: string[];
  initialFen?: string;
}

export interface Matchup {
  white: string;
  black: string;
}

interface Script extends Test {
  games: Matchup[];
}

export type Glicko = { r: number; rd: number };

type DevRatings = { [speed in LocalSpeed]?: Glicko };

export class DevCtrl {
  hurryProp: Prop<boolean> = storedBooleanProp('local.dev.hurry', false);
  // skip animations, sounds, and artificial think times (clock still adjusted)
  script: Script;
  log: Result[];
  ratings: { [uid: string]: DevRatings } = {};
  private localRatings: ObjectStorage<DevRatings>;

  constructor() {}

  async init(): Promise<void> {
    this.resetScript();
    await this.getStoredRatings();
    pubsub.on('theme', env.redraw);
  }

  get hurry(): boolean {
    return this.hurryProp() || (this.gameInProgress && env.bot.playing.some(x => 'level' in x));
  }

  run(test?: Test, iterations: number = 1): boolean {
    if (test) {
      this.resetScript(test);
      this.script.games.push(...this.matchups(test, iterations));
    }
    const game = this.script.games.shift();
    if (!game) return false;
    env.game.reset({ ...game, initialFen: env.game.initialFen });
    env.redraw();
    env.game.start();
    return true;
  }

  resetScript(test?: Test): void {
    this.log ??= [];
    const players = [env.game.white, env.game.black].filter(x => defined(x)) as string[];
    this.script = {
      type: 'matchup',
      players,
      games: [],
      ...test,
    };
  }

  onReset(): void {}

  preMove(moveResult: MoveContext): void {
    env.round.chessground?.set({ animation: { enabled: !this.hurry } });
    if (this.hurry) moveResult.silent = true;
  }

  onGameOver({ winner, turn, reason, status }: GameStatus): boolean {
    const last = { winner, white: this.white?.uid, black: this.black?.uid };
    this.log.push(last);
    if (status === statusOf('cheat')) {
      console.error(
        `${env.game.nameOf('white')} vs ${env.game.nameOf('black')} - ${turn} ${reason} - ${
          env.game.live.fen
        } ${env.game.live.moves.join(' ')}`,
        stringify(env.game.live.chess),
      );
      return false;
    } else
      console.info(
        `game ${this.log.length} ${env.game.nameOf('white')} vs ${env.game.nameOf('black')}:`,
        winner ? `${env.game.nameOf(winner)} wins by` : 'draw',
        status.name,
        reason ?? '',
      );
    if (!this.white?.uid || !this.black?.uid) return false;
    this.updateRatings(this.white.uid, this.black.uid, winner);

    if (this.script.type === 'rate') {
      const uid = this.script.players[0]!;
      const rating = this.getRating(uid, env.game.speed);
      this.script.games.push(...rateBotMatchup(uid, rating, last));
    }
    if (this.testInProgress) return this.run();
    this.resetScript();
    env.redraw();
    return false;
  }

  getRating(uid: string | undefined, speed: LocalSpeed): Glicko {
    if (!uid) return { r: 1500, rd: 350 };
    const bot = env.bot.get(uid);
    if (bot instanceof RateBot) return { r: bot.ratings[speed], rd: 0.01 };
    else return this.ratings[uid]?.[speed] ?? { r: 1500, rd: 350 };
  }

  setRating(uid: string | undefined, speed: LocalSpeed, rating: Glicko): Promise<any> {
    if (!uid || !env.bot.bots[uid]) return Promise.resolve();
    this.ratings[uid] ??= {};
    this.ratings[uid][speed] = rating;
    return this.localRatings.put(uid, this.ratings[uid]);
  }

  get hasUser(): boolean {
    return !(this.white && this.black);
  }

  get gameInProgress(): boolean {
    return env.game.live.ply > 0 && !env.game.live.status.end;
  }

  async clearRatings(): Promise<void> {
    await this.localRatings.clear();
    this.ratings = {};
  }

  private matchups(test: Test, iterations = 1): Matchup[] {
    const players = test.players;
    if (players.length < 2) return [];
    if (test.type === 'rate') {
      const rating = this.getRating(players[0], env.game.speed);
      return rateBotMatchup(players[0], rating);
    }
    const games: Matchup[] = [];
    for (let it = 0; it < iterations; it++) {
      if (test.type === 'roundRobin') {
        const tourney: Matchup[] = [];
        for (let i = 0; i < players.length; i++) {
          for (let j = i + 1; j < players.length; j++) {
            tourney.push({ white: players[i], black: players[j] });
            tourney.push({ white: players[j], black: players[i] });
          }
        }
        shuffle(tourney);
        games.push(...tourney);
      } else games.push({ white: test.players[it % 2], black: test.players[(it + 1) % 2] });
    }
    return games;
  }

  private async getStoredRatings(): Promise<void> {
    if (!this.localRatings)
      this.localRatings = await objectStorage<DevRatings>({ store: 'local.bot.ratings' });
    const keys = await this.localRatings.list();
    this.ratings = Object.fromEntries(
      await Promise.all(keys.map(k => this.localRatings.get(k).then(v => [k, v]))),
    );
  }

  private updateRatings(whiteUid: string, blackUid: string, winner: Color | undefined): Promise<any> {
    const whiteScore = winner === 'white' ? 1 : winner === 'black' ? 0 : 0.5;
    const rats = [whiteUid, blackUid].map(uid => this.getRating(uid, env.game.speed));

    return Promise.all([
      this.setRating(whiteUid, env.game.speed, updateGlicko(rats, whiteScore)),
      this.setRating(blackUid, env.game.speed, updateGlicko(rats.reverse(), 1 - whiteScore)),
    ]);

    function updateGlicko(glk: Glicko[], score: number): Glicko {
      const q = Math.log(10) / 400;
      const expected = 1 / (1 + 10 ** ((glk[1].r - glk[0].r) / 400));
      const g = 1 / Math.sqrt(1 + (3 * q ** 2 * glk[1].rd ** 2) / Math.PI ** 2);
      const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
      const deltaR = glk[0].rd <= 0 ? 0 : (q * g * (score - expected)) / (1 / dSquared + 1 / glk[0].rd ** 2);
      return {
        r: Math.round(glk[0].r + deltaR),
        rd: Math.max(30, Math.sqrt(1 / (1 / glk[0].rd ** 2 + 1 / dSquared))),
      };
    }
  }

  private get white(): BotInfo | undefined {
    return env.bot.white;
  }

  private get black(): BotInfo | undefined {
    return env.bot.black;
  }

  private get testInProgress(): boolean {
    return this.script.games.length !== 0;
  }
}
