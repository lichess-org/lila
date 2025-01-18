import { clamp } from 'common/algo';
import { botScore } from './devUtil';
import type { BotInfo, MoveSource, MoveResult, MoveArgs, Book, Ratings } from '../types';
import type { Result, Matchup, Glicko } from './devCtrl';
import { env } from '../localEnv';

// ratings in context of classical Computer Chess Rating Lists (CCRL) where goldfish 1.13 = 2000
// goldfish is an intentionally weakened Stockfish derivative for an approximate bridge to FIDE Elo.
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
  traceMove: string;

  constructor(readonly level: number) {
    const rating = (this.level + 8) * 75;
    this.ratings = {
      ultraBullet: rating,
      bullet: rating,
      blitz: rating,
      rapid: rating,
      classical: rating,
    };
    Object.defineProperty(this, 'traceMove', { value: '', writable: true });
  }

  get uid(): string {
    return `#${this.level}`;
  }

  get depth(): number {
    return clamp(this.level - 9, { min: 1, max: 20 });
  }

  get description(): string {
    return `Stockfish ${this.ratings.classical} Skill Level ${this.level - 10} Depth ${this.depth}`;
  }

  async move({ pos }: MoveArgs): Promise<MoveResult> {
    const ply = env.game.live.ply;
    const turn = env.game.live.turn;
    const fen = pos.fen;
    const uci = (
      await env.bot.zerofish.goFish(pos, { multipv: 1, level: this.level - 10, by: { depth: this.depth } })
    ).bestmove;
    this.traceMove = `  ${ply}. '${this.name} ${this.ratings.classical}' at '${fen}': '${uci}'`;
    return { uci, thinkTime: 0.2 };
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
