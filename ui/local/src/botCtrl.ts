import type { Zerofish, Position, FishSearch } from 'zerofish';
import { Libot, Libots } from './interfaces';
import { ZerofishBot } from './zerofishBot';
import { Database } from './database';

export class BotCtrl {
  zf: Zerofish;
  bots: Libots;
  players: { white?: Libot; black?: Libot } = {};
  rankBot: RankBot;

  constructor(
    inbots: any,
    zf: Zerofish,
    readonly db: Database,
  ) {
    this.zf = zf;
    this.bots = {};
    this.rankBot = new RankBot(zf);
    for (const bot of inbots) {
      this.bots[bot.uid] = new ZerofishBot(bot, this.db, zf);
    }
  }

  async setBot(color: Color, uid?: string | number) {
    if (typeof uid === 'number') {
      this.rankBot.level = uid as number;
    } else this.players[color] = uid ? this.bots[uid] : undefined;
  }

  swap() {
    [this.players.white, this.players.black] = [this.players.black, this.players.white];
  }

  move(pos: Position, color: Color): Promise<string> {
    return this.players[color]?.move(pos) ?? Promise.resolve('0000');
  }

  sfRank(level: number) {
    return level < 10 ? level * 80 : level * 100 - 200;
  }

  stop() {
    this.zf.stop();
  }
}

class RankBot implements Libot {
  level = 30;
  name = 'Stockfish';
  uid = '#stockfish';
  ratings = new Map();
  image = 'baby-robot.webp';

  constructor(readonly zf: Zerofish) {}
  get description() {
    return `Stockfish rank bot ${this.level}`;
  }

  get imageUrl() {
    return site.asset.url(`lifat/bots/images/${this.image}`, { version: 'bot000' });
  }

  async move(pos: Position) {
    return (await this.zf.goFish(pos, { level: this.level })).bestmove;
  }
}
