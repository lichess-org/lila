import { Puzzle } from './interfaces';
import { getNow } from './util';

export default class CurrentPuzzle {
  line: Uci[];
  startAt: number;
  moveIndex = -1;
  pov: Color;

  constructor(
    readonly index: number,
    readonly puzzle: Puzzle,
  ) {
    this.line = puzzle.line.split(' ');
    this.pov = co.opposite(co.fen.parseFen(puzzle.fen).unwrap().turn);
    this.startAt = getNow();
  }

  position = () => {
    const pos = co.Chess.fromSetup(co.fen.parseFen(this.puzzle.fen).unwrap()).unwrap();
    if (this.moveIndex >= 0)
      this.line.slice(0, this.moveIndex + 1).forEach(uci => pos.play(co.parseUci(uci)!));
    return pos;
  };

  expectedMove = () => this.line[this.moveIndex + 1];

  lastMove = () => this.line[this.moveIndex];

  isOver = () => this.moveIndex >= this.line.length - 1;
}
