import { clamp } from 'common';
import { botScore } from './util';
import type { Zerofish, Position } from 'zerofish';
import type { Libot, Result, Matchup } from '../types';

export class RankBot implements Libot {
  static readonly MAX_LEVEL = 30;

  image = 'baby-robot.webp';

  constructor(
    readonly zerofish: Zerofish,
    readonly level: number,
  ) {}

  get uid() {
    return `#${this.level}`;
  }

  get name() {
    return `Stockfish`;
  }

  get glicko() {
    return { r: (this.level + 8) * 75, rd: 20 };
  }
  get ratingText() {
    return `${this.glicko.r}`;
  }
  get depth() {
    return clamp(this.level - 9, { min: 1, max: 20 });
  }

  get description() {
    return `Stockfish UCI_Elo ${this.glicko.r} depth ${this.depth}`;
  }

  async move(pos: Position) {
    return (await this.zerofish.goFish(pos, { multipv: 1, level: this.level, by: { depth: this.depth } }))
      .bestmove;
  }
}

export function rankBotMatchup(bot: Libot, last?: Result): Matchup[] {
  const { r, rd } = bot.glicko ?? { r: 1500, rd: 350 };
  if (rd < 60) return [];
  const score = last ? botScore(last, bot.uid) : 0.5;
  const lvl = rankBotLevel(r + (Math.random() + score - 1) * (rd * 1.5));
  return [Math.random() < 0.5 ? { white: bot.uid, black: `#${lvl}` } : { white: `#${lvl}`, black: bot.uid }];
}

function rankBotLevel(rating: number) {
  return clamp(Math.round(rating / 75) - 8, { min: 0, max: RankBot.MAX_LEVEL });
}
