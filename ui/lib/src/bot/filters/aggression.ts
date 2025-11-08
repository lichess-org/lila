import * as co from 'chessops';
import type { SearchMove, MoveArgs, FilterInfo } from '../types';
import { Bot } from '../bot';
import { normalMove } from '@/game';

const info: FilterInfo = {
  label: 'aggression',
  type: 'filter',
  class: ['filter'],
  value: { range: { min: -1, max: 1 }, by: 'avg' },
  requires: {
    some: [
      'behavior_fish_multipv > 1',
      'behavior_zero_multipv > 1',
      { every: ['behavior_zero', 'behavior_fish'] },
    ],
  },
  title: $trim`
    aggression assigns weights to moves that remove opponent material from the board.

    a value of 1 will increase the likelihood of captures, 0 is neutral, and -1 will avoid
    captures.
    
    this one should be combined with other filters.`,
};

Bot.registerFilter('aggression', { info, score });

function score(moves: SearchMove[], args: MoveArgs, limiter: number): void {
  for (const mv of moves) {
    const chess = args.chess.clone();
    const normal = normalMove(chess, mv.uci)!.move;
    mv.weights.aggressionBias = co.san.makeSan(chess, normal).includes('x') ? limiter : 0;
  }
}
