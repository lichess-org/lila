import * as co from 'chessops';
import type { SearchMove, MoveArgs } from '../types';
import type { FilterResult } from '../filter';
import { Bot } from '../bot';
import { normalMove } from '@/game';

Bot.registerFilter('aggression', {
  info: {
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
  },
  score: (moves: SearchMove[], args: MoveArgs, limiter: number): FilterResult => {
    const result: FilterResult = {};
    for (const { uci } of moves) {
      const chess = args.chess.clone();
      const normal = normalMove(chess, uci)!.move;
      result[uci] = co.san.makeSan(chess, normal).includes('x') ? limiter : 0;
    }
    return result;
  },
});
