import { Shogi, opposite, parseUsi } from 'shogiops';
import { parseFen } from 'shogiops/fen';
import { Puzzle } from './interfaces';
import { getNow } from './util';

export default class CurrentPuzzle {
  line: Uci[];
  startAt: number;
  moveIndex: number = 0;
  pov: Color;

  constructor(readonly index: number, readonly puzzle: Puzzle) {
    this.line = puzzle.line.split(' ');
    this.pov = opposite(parseFen(puzzle.fen).unwrap().turn);
    this.startAt = getNow();
  }

  position = (): Shogi => {
    const pos = Shogi.fromSetup(parseFen(this.puzzle.fen).unwrap()).unwrap();
    this.line.slice(0, this.moveIndex + 1).forEach(uci => pos.play(parseUsi(uci)!));
    return pos;
  };

  expectedMove = () => this.line[this.moveIndex + 1];

  lastMove = () => this.line[this.moveIndex];

  isOver = () => this.moveIndex >= this.line.length - 1;
}
