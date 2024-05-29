import type { Zerofish, Position, FishSearch } from 'zerofish';
import { Libot, Libots, Result, Matchup } from './interfaces';
import { ZerofishBot } from './zerofishBot';
import { Database } from './database';
import { clamp } from 'common';

export class BotCtrl {
  zf: Zerofish;
  bots: Libots;
  players: { white?: Libot; black?: Libot } = {};
  private rankBot: RankBot;
  private rankBots: RankBot[] = [];
  private player: Libot = {
    name: 'Player',
    uid: 'player',
    description: 'Human player',
    image: 'gray-torso.webp',
    imageUrl: site.asset.url('lifat/bots/images/gray-torso.webp', { version: 'bot000' }),
    ratings: new Map(),
    move: () => Promise.resolve('0000'),
  };
  readonly RANK_MAX = 30;

  constructor(
    inbots: Libot[],
    zf: Zerofish,
    readonly db: Database,
  ) {
    this.zf = zf;
    this.bots = {};
    for (let i = 0; i <= this.RANK_MAX; i++) this.rankBots.push(new RankBot(zf, i));
    this.rankBot = this.rankBots[this.RANK_MAX];
    for (const bot of inbots) {
      this.bots[bot.uid] = new ZerofishBot(bot, this.db, zf);
    }
    zf.fish(`setoption name Threads value ${navigator.hardwareConcurrency - 1}`, 1);
  }

  async setBot(color: Color, uid?: string | number) {
    this.players[color] = uid === undefined ? undefined : this.getBot(uid);
  }

  getBot(uid: string | number | undefined) {
    if (uid === undefined) return { ...this.player, uid: 'player' };
    if (typeof uid === 'number') return this.rankBots[uid];
    else if (this.isRankBot(uid)) return this.rankBots[Number(uid.slice(1))];
    return this.bots[uid] ?? { ...this.player, uid };
  }

  isRankBot(uid?: string) {
    return uid && !isNaN(Number(uid.slice(1)));
  }

  swap() {
    [this.players.white, this.players.black] = [this.players.black, this.players.white];
  }

  move(pos: Position, color: Color): Promise<string> {
    return this.players[color]?.move(pos) ?? Promise.resolve('0000');
  }

  stop() {
    this.zf.stop();
  }

  get white() {
    return this.players.white ?? this.player;
  }

  get black() {
    return this.players.black ?? this.player;
  }

  rankNext(results: Result[]): { matchup?: Matchup; rating: number } {
    const bot = (this.isRankBot(this.white.uid) ? this.black : this.white) as Libot;
    const playerColor = (m: Result) => (this.isRankBot(m.white) ? 'black' : 'white');
    const playerWin = (m: Result) => m.result === playerColor(m);
    const playerScore = (m: Result) => (playerWin(m) ? 1 : m.result === 'draw' ? 0.5 : 0);
    const rankBotColor = (m: Result) => (this.isRankBot(m.white) ? 'white' : 'black');
    const rankBotElo = (m: Result) => elo(this.getBot(m[rankBotColor(m)]).level!);
    const elo = (level: number) => (level + 8) * 75;
    const total = results.length;
    const player = results.reduce(
      (p, r) => {
        const rankBot = this.getBot(r[rankBotColor(r)]);
        const level = rankBot.level!;
        const rankElo = elo(level);
        const expected = 1 / (1 + 10 ** ((rankElo - p.rating) / 400));
        const newElo = p.rating + p.k * (playerScore(r) - expected);
        return { rating: newElo, k: p.k * 0.96 };
      },
      { rating: 1725, k: 100 },
    );
    const r = results[results.length - 1];
    const score = playerScore(r) - 0.5;
    console.log(
      `${bot.uid} ${
        playerScore(r) === 1 ? 'wins' : playerScore(r) === 0.5 ? 'draws' : 'loses'
      } vs ${rankBotElo(r)}. Now ranked at ${Math.round(player.rating)} in ${total} games`,
    );

    const matchup =
      total >= 30
        ? undefined
        : this.rankMatchup(
            clamp(Math.round(player.rating / 75) - 8 + ((Math.random() - 0.5 + score) * (30 - total)) / 3, {
              min: 0,
              max: 30,
            }),
            bot,
          );
    if (!matchup) {
      console.log(`Ranking complete. ${Math.round(player.rating)}`);
      bot.ratings.set('classical', player.rating);
    }
    return { matchup, rating: player.rating };
  }

  rankMatchup(rank: number, bot: Libot) {
    rank = clamp(Math.round(rank), { min: 0, max: this.RANK_MAX });
    const matchup = { white: bot.uid, black: `#${rank}` };
    if (Math.random() < 0.5) [matchup.white, matchup.black] = [matchup.black, matchup.white];
    return matchup;
  }
}
class RankBot implements Libot {
  ratings = new Map<Speed, number>();
  imageUrl = site.asset.url('lifat/bots/images/baby-robot.webp', { version: 'bot000' });

  constructor(
    readonly zf: Zerofish,
    readonly level: number,
  ) {
    //this.ratings.set('blitz', ...);
    //this.ratings.set('rapid', ...);
    this.ratings.set('classical', this.elo); // 60s+0.6s, but whatevs
  }

  get uid() {
    return `#${this.level}`;
  }
  get name() {
    return `Stockfish ${this.elo}`;
  }
  get elo() {
    // https://github.com/official-stockfish/Stockfish/blob/9587eeeb5ed29f834d4f956b92e0e732877c47a7/src/search.cpp#L331
    // stockfish 'Skill Level' uci option = rankbot level - 10
    // rankbot 10 -> 1350 depth 1, rankbot 29 -> 2775 depth 20
    return (this.level + 8) * 75;
  }
  get depth() {
    return clamp(this.level - 9, { min: 1, max: 20 }); // also from search.cpp
  }
  get description() {
    return `Stockfish rank bot ${this.level}`;
  }

  async move(pos: Position) {
    return (await this.zf.goFish(pos, { level: this.level, depth: this.depth }, 1)).bestmove;
  }
}
