import { IncompleteLevel, Level, Score, UsiWithColor } from './interfaces';

export function createLevel(l: IncompleteLevel, it: number): Level {
  return {
    id: it + 1,
    color: l.sfen.includes(' b') ? 'sente' : 'gote',
    ...l,
  };
}

// assumes level is completed
export function calcScore(l: Level, usiCList: UsiWithColor[]): Score {
  const nbMoves = usiCList.filter(uc => uc.color === l.color).length;
  if (nbMoves <= l.nbMoves) return 3;
  else if (nbMoves - 1 <= l.nbMoves) return 2;
  else return 1;
}
