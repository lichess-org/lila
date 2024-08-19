import { clamp } from 'common';
import { botScore } from './devUtil';
import type { BotInfo, Mover, MoveResult, MoveArgs, Book, Ratings } from '../types';
import type { Result, Matchup, Glicko } from './devCtrl';
import { env } from '../localEnv';

export class RateBot implements BotInfo, Mover {
  static readonly MAX_LEVEL = 30;

  image = '3d4c495c229b.webp';
  version = 0;
  name = 'Stockfish';
  ratings: Ratings;
  books: Book[] = [];
  sounds = {};
  operators = {};

  constructor(readonly level: number) {
    const rating = (this.level + 8) * 75;
    this.ratings = {
      ultraBullet: rating, // TODO: figure out non-classical rateBot levels
      bullet: rating,
      blitz: rating,
      rapid: rating,
      classical: rating,
    };
  }

  get uid(): string {
    return `#${this.level}`;
  }

  get depth(): number {
    return clamp(this.level - 9, { min: 1, max: 20 });
  }

  get description(): string {
    return `Stockfish UCI_Elo ${this.ratings.classical} depth ${this.depth}`;
  }

  async move({ pos }: MoveArgs): Promise<MoveResult> {
    return {
      uci: (await env.bot.zerofish.goFish(pos, { multipv: 1, level: this.level, by: { depth: this.depth } }))
        .bestmove,
      thinktime: 0,
    };
  }

  thinking(): number {
    return 1;
  }
}

export function rateBotMatchup(uid: string, { r, rd }: Glicko, last?: Result): Matchup[] {
  if (rd < 60) return [];
  const score = last ? botScore(last, uid) : 0.5;
  const lvl = ratingToRateBotLevel(r + (Math.random() + score - 1) * (rd * 1.5));
  return [Math.random() < 0.5 ? { white: uid, black: `#${lvl}` } : { white: `#${lvl}`, black: uid }];
}

function ratingToRateBotLevel(rating: number) {
  return clamp(Math.round(rating / 75) - 8, { min: 0, max: RateBot.MAX_LEVEL });
}
