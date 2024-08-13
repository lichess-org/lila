import { clamp } from 'common';
import { botScore } from './devUtil';
import type { Zerofish } from 'zerofish';
import type { BotInfo, Glicko, Mover, MoveResult, MoveArgs, Ratings, Book } from '../types';
import type { Result, Matchup } from './devCtrl';

export class RateBot implements BotInfo, Mover {
  static readonly MAX_LEVEL = 30;

  image = 'baby-robot.webp';
  version = 0;
  name = 'Stockfish';
  ratings: Ratings;
  books: Book[] = [];
  sounds = {};
  operators = {};

  constructor(
    readonly zerofish: Zerofish,
    readonly level: number,
  ) {
    const glicko = { r: (this.level + 8) * 75, rd: 0.1 };
    this.ratings = {
      ultraBullet: glicko, // TODO: figure out non-classical
      bullet: glicko,
      blitz: glicko,
      rapid: glicko,
      classical: glicko,
    };
  }

  get uid(): string {
    return `#${this.level}`;
  }
  get ratingText(): string {
    return `${this.ratings.classical!.r}`;
  }
  get depth(): number {
    return clamp(this.level - 9, { min: 1, max: 20 });
  }

  get description(): string {
    return `Stockfish UCI_Elo ${this.ratings.classical!.r} depth ${this.depth}`;
  }

  async move({ pos }: MoveArgs): Promise<MoveResult> {
    return {
      uci: (await this.zerofish.goFish(pos, { multipv: 1, level: this.level, by: { depth: this.depth } }))
        .bestmove,
      thinktime: 0,
    };
  }

  thinking(): number {
    return 1;
  }
}

export function rateBotMatchup(uid: string, { r, rd }: Glicko, last?: Result): Matchup[] {
  console.log('rateBotMatchup', uid, r, rd, last);
  if (rd < 60) return [];
  const score = last ? botScore(last, uid) : 0.5;
  const lvl = ratingToRateBotLevel(r + (Math.random() + score - 1) * (rd * 1.5));
  return [Math.random() < 0.5 ? { white: uid, black: `#${lvl}` } : { white: `#${lvl}`, black: uid }];
}

function ratingToRateBotLevel(rating: number) {
  return clamp(Math.round(rating / 75) - 8, { min: 0, max: RateBot.MAX_LEVEL });
}
