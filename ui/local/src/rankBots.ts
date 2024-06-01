import { Zerofish, Position } from 'zerofish';
import { clamp } from 'common';
import * as co from 'chessops';
import { Libot, Result, Matchup } from './types';
import { BotCtrl } from './botCtrl';

const NUM_GAMES = 30;

export function rateMatchup(level: number, bot: Libot) {
  level = clamp(Math.round(level), { min: 0, max: RankBot.MAX_LEVEL });
  const matchup = { white: bot.uid, black: `#${level}` };
  if (Math.random() < 0.5) [matchup.white, matchup.black] = [matchup.black, matchup.white];
  return matchup;
}

export function rateAndNext(ctrl: BotCtrl, results: Result[]): { matchup?: Matchup; rating: number } {
  const bot = (ctrl.players.white?.isRankBot ? ctrl.players.black : ctrl.players.white) as Libot;
  const botColor = (m: Result) => (m.white === bot.uid ? 'white' : 'black');
  const rankBotColor = (m: Result) => co.opposite(botColor(m));
  const rankBot = (m: Result) => ctrl.bot(m[rankBotColor(m)]) as RankBot;
  const botWin = (m: Result) => m.result === botColor(m);
  const botScore = (m: Result) => (botWin(m) ? 1 : m.result === 'draw' ? 0.5 : 0);
  const q = Math.log(10) / 400;
  const { r: rating } = results.reduce(
    ({ r, rd }: { r: number; rd: number }, result: Result) => {
      const expected = 1 / (1 + 10 ** ((rankBot(result).rating - r) / 400));
      const g = 1 / Math.sqrt(1 + (3 * q ** 2 * rd ** 2) / Math.PI ** 2);
      const dSquared = 1 / (q ** 2 * g ** 2 * expected * (1 - expected));
      const deltaR = (q / (1 / dSquared + 1 / rd ** 2)) * (g * (botScore(result) - expected));
      return { r: r + deltaR, rd: Math.sqrt(1 / (1 / rd ** 2 + 1 / dSquared)) };
    },
    { r: 1725, rd: 350 },
  );
  bot.updateRating?.(rating);
  if (results.length >= NUM_GAMES) return { rating };
  const nextLevel = ratingToLevel(
    rating +
      (Math.random() + botScore(results[results.length - 1]) - 1) *
        800 * // +/- 400
        (1 - results.length / NUM_GAMES) ** 2,
  );
  return { matchup: rateMatchup(nextLevel, bot), rating };
}

export class RankBot implements Libot {
  imageUrl = site.asset.url('lifat/bots/images/baby-robot.webp', { version: 'bot000' });
  isRankBot = true;
  static readonly MAX_LEVEL = 30;
  constructor(
    readonly zf: Zerofish,
    readonly level: number,
  ) {}

  get uid() {
    return `#${this.level}`;
  }
  get name() {
    return `Stockfish`;
  }
  get rating() {
    return (this.level + 8) * 75;
  }
  get depth() {
    return clamp(this.level - 9, { min: 1, max: 20 });
  }
  get description() {
    return `Stockfish UCI_Elo ${this.rating} depth ${this.depth}`;
  }
  async move(pos: Position) {
    return (await this.zf.goFish(pos, { level: this.level, depth: this.depth }, 1)).bestmove;
  }
}

function ratingToLevel(rating: number) {
  return clamp(Math.round(rating / 75) - 8, { min: 0, max: RankBot.MAX_LEVEL });
}
