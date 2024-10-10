import { clamp } from 'common/algo';
import { botScore } from './devUtil';
import type { BotInfo, MoveSource, MoveResult, MoveArgs, Book, Ratings } from '../types';
import type { Result, Matchup, Glicko } from './devCtrl';
import { env } from '../localEnv';

// by virtue of the Stockfish implementation of the UCI_Elo parameter, all measured ratings are
// anchored to classical Computer Chess Rating Lists (CCRL) where goldfish 1.13 = 2000
// goldfish is an intentionally weakened Stockfish derivative that (AFAICT) serves as an approximate
// bridge to FIDE Elo.
//
// https://github.com/official-stockfish/Stockfish/blob/9587eeeb5ed29f834d4f956b92e0e732877c47a7/src/search.cpp#L333

export class RateBot implements BotInfo, MoveSource {
  static readonly MAX_LEVEL = 29;

  image = '3d4c495c229b.webp';
  version = 0;
  name = 'Stockfish';
  ratings: Required<Ratings>;
  books: Book[] = [];
  sounds = {};
  filters = {};

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
    // this might be redundant due to
    // https://github.com/official-stockfish/Stockfish/blob/9587eeeb5ed29f834d4f956b92e0e732877c47a7/src/search.cpp#L99
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
