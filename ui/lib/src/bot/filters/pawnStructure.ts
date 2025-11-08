import * as co from 'chessops';
import type { SearchMove, MoveArgs, FilterInfo } from '../types';
import { Bot } from '../bot';
import { clamp } from '@/algo';
import { normalMove } from '@/game';

const info: FilterInfo = {
  label: 'pawn structure',
  type: 'filter',
  class: ['filter'],
  value: { range: { min: 0, max: 1 }, by: 'avg' },
  requires: {
    some: [
      'behavior_fish_multipv > 1',
      'behavior_zero_multipv > 1',
      { every: ['behavior_zero', 'behavior_fish'] },
    ],
  },
  title: $trim`
    pawn structure assigns weights up to the graph value for pawns that support each other, control the center,
    and are not doubled or isolated.
    
    This filter assigns a weight between 0 and 1.`,
};

Bot.registerFilter('pawnStructure', { info, score });

function score(moves: SearchMove[], args: MoveArgs, limiter: number): void {
  for (const mv of moves) {
    const chess = args.chess.clone();
    chess.play(normalMove(chess, mv.uci)!.move);
    const score = pawnStructure(chess);
    mv.weights.pawnStructure = clamp(score * limiter, { min: 0, max: 1 });
  }
  const grouped = moves.reduce((groups, mv) => {
    const group = groups.get(mv.weights.pawnStructure!) ?? [];
    group.push(mv);
    groups.set(mv.weights.pawnStructure!, group);
    return groups;
  }, new Map<number, SearchMove[]>());
  const vals = [...grouped.keys()].sort((a, b) => b - a);
  vals.forEach((val, i) => {
    for (const mv of grouped.get(val)!) {
      mv.weights.pawnStructure = (vals.length - i) / vals.length;
    }
  });
}

// michael's python algorithm: https://hq.lichess.ovh/#narrow/channel/8-dev/topic/Fancy.20Bots/near/3803327

function pawnStructure(b: co.Position): number {
  const color: Color = co.opposite(b.turn);
  const pawns: co.SquareSet = b.board.pieces(color, 'pawn');
  const fc: Record<number, number> = {};
  const pos: [number, number][] = [];
  let score = 0;

  // Advancement score
  for (const sq of pawns) {
    const f = co.squareFile(sq),
      r = co.squareRank(sq);
    fc[f] = (fc[f] ?? 0) + 1;
    pos.push([f, r]);
    score += (color === 'white' ? r : 7 - r) / 7.0;
  }
  // Penalize doubled pawns
  for (const c of Object.values(fc)) if (c > 1) score -= 0.75 * (c - 1);

  // Reward tightly connected pawns (within 1 square including diagonally)
  let conn = 0;
  for (let i = 0; i < pos.length; i++) {
    const [fx, fy] = pos[i];
    for (let j = i + 1; j < pos.length; j++) {
      const [sx, sy] = pos[j];
      if (Math.abs(fx - sx) === 1 && Math.abs(fy - sy) <= 1) conn++;
    }
  }

  score += 0.5 * conn;

  return Math.max(0, Math.min(1, (score + 2) / 12.5));
}
