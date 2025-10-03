import { RateBot, rateBotMatchup } from './rateBot';
import type { BotInfo, LocalSpeed } from 'lib/bot/types';
import { statusOf } from 'lib/game/game';
import { defined, type Prop } from 'lib';
import { shuffle } from 'lib/algo';
import { type ObjectStorage, objectStorage } from 'lib/objectStorage';
import { storedBooleanProp } from 'lib/storage';
import type { GameStatus, GameContext } from './localGame';
import { env } from './devEnv';
import { pubsub } from 'lib/pubsub';
import { type PermaLog, makeLog } from 'lib/permalog';
import type { GameObserver } from './gameCtrl';

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

export class DevCtrl implements GameObserver {
  hurryProp: Prop<boolean> = storedBooleanProp('botdev.hurry', false);
  // skip animations, sounds, and artificial think times (clock still adjusted)
  script: Script;
  log: Result[];
  private traceDb: PermaLog;
  private trace: string[] = [];
  ratings: { [uid: string]: DevRatings } = {};
  private localRatings: ObjectStorage<DevRatings>;

  async init(): Promise<void> {
    this.resetScript();
    [this.traceDb] = await Promise.all([makeLog({ store: 'botmove' }, 1), this.getStoredRatings()]);
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
    env.game.load({ ...game, setupFen: env.game.live.setupFen });
    env.redraw();
    env.game.start();
    return true;
  }

  resetScript(test?: Test): void {
    this.log ??= [];
    this.trace = [];
    const players = [env.game.white, env.game.black].filter(x => defined(x)) as string[];
    this.script = {
      type: 'matchup',
      players,
      games: [],
      ...test,
    };
  }

  onReset(): void {}

  beforeMove(uci: string): void {
    const ply = env.game.live.ply;
    const fen = env.game.live.fen;
    const turn = env.game.live.turn;
    if (ply === 0) {
      const white = env.game.nameOf('white');
      const black = env.game.nameOf('black');
      const stringify = (obj: any) =>
        JSON.stringify(obj, (_, v) => (!obj ? '' : typeof v === 'number' ? v.toFixed(2) : v));
      this.trace.push(
        `\n${white} vs ${black} ${env.game.speed} ${env.game.initial ?? ''}` +
          `${env.game.increment ? `-${env.game.increment}` : ''} ${env.game.live.initialFen}`,
      );
      this.trace.push(`\nWhite: '${white}' ${env.bot.white ? stringify(env.bot.white) : ''}`);
      this.trace.push(`Black: '${black}' ${env.bot.black ? stringify(env.bot.black) : ''}`);
    }
    if (ply % 2 === 0) this.trace.push(`\n ${'-'.repeat(64)} Move ${ply / 2 + 1} ${'-'.repeat(64)}`);
    if (!env.bot[turn]) this.trace.push(`  ${ply}. '${env.game.nameOf(turn)}' at '${fen}': '${uci}'`);
  }

  afterMove(moveResult: GameContext): void {
    const lastColor = env.game.live.awaiting;
    env.round.chessground?.set({ animation: { enabled: !this.hurry } });
    if (this.hurry) moveResult.silent = true;
    const trace = env.bot[lastColor]?.traceMove;
    if (trace) this.trace.push(trace);
  }

  onGameOver({ winner, reason, status }: GameStatus): boolean {
    const last = { winner, white: this.white?.uid, black: this.black?.uid };
    this.log.push(last);
    const matchup = `${env.game.live.id} '${env.game.nameOf('white')}' vs '${env.game.nameOf('black')}'`;
    const error =
      status === statusOf('unknownFinish') &&
      `${matchup} - ${env.game.live.turn} ${reason} - ${env.game.live.fen} ${env.game.live.moves.join(' ')}`;
    const result = `${matchup}:${winner ? ` ${env.game.nameOf(winner)} wins by` : ''} ${status.name} ${reason ?? ''}`;
    this.trace.push(`\n ${error || result}\n`);
    this.trace.push('='.repeat(144));
    this.traceDb(this.trace.join('\n'));
    this.trace = [];

    console.log(`game ${this.log.length} - ` + (error || result));

    if (error || !this.white?.uid || !this.black?.uid) return false;
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
    const bot = env.bot.info(uid);
    if (bot instanceof RateBot) return { r: bot.ratings[speed], rd: 0.01 };
    else return this.ratings[uid]?.[speed] ?? { r: 1500, rd: 350 };
  }

  setRating(uid: string | undefined, speed: LocalSpeed, rating: Glicko): Promise<any> {
    if (!uid || !env.bot.bots.has(uid)) return Promise.resolve();
    this.ratings[uid] ??= {};
    this.ratings[uid][speed] = rating;
    return this.localRatings.put(uid, this.ratings[uid]);
  }

  async getTrace(): Promise<string> {
    return (await this.traceDb.get()) + '\n' + this.trace.join('\n');
  }

  get hasUser(): boolean {
    return !(this.white && this.black);
  }

  get gameInProgress(): boolean {
    return !!env.game.rewind || (env.game.live.ply > 0 && !env.game.live.finished);
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
        games.push(...shuffle(tourney));
      } else games.push({ white: test.players[it % 2], black: test.players[(it + 1) % 2] });
    }
    return games;
  }

  private async getStoredRatings(): Promise<void> {
    if (!this.localRatings)
      this.localRatings = await objectStorage<DevRatings>({ store: 'botdev.bot.ratings' });
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
