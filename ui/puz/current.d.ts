/// <reference types="lishogi" />
import { Shogi } from 'shogiops';
import { Puzzle } from './interfaces';
export default class CurrentPuzzle {
  readonly index: number;
  readonly puzzle: Puzzle;
  line: Uci[];
  startAt: number;
  moveIndex: number;
  pov: Color;
  constructor(index: number, puzzle: Puzzle);
  position: () => Shogi;
  expectedMove: () => string;
  lastMove: () => string;
  isOver: () => boolean;
}
