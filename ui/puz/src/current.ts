import { Shogi } from 'shogiops/shogi';
import { parseSfen } from 'shogiops/sfen';
import { parseUsi } from 'shogiops/util';
import { Puzzle } from './interfaces';
import { getNow } from './util';
import { pretendItsUsi } from 'common';

export default class CurrentPuzzle {
  line: Usi[];
  startAt: number;
  moveIndex: number = 0;
  pov: Color;

  constructor(readonly index: number, readonly puzzle: Puzzle) {
    this.line = puzzle.line.split(' ');
    this.pov = parseSfen(puzzle.sfen).unwrap().turn;
    this.startAt = getNow();
  }

  position = (): Shogi => {
    const pos = Shogi.fromSetup(parseSfen(this.puzzle.sfen).unwrap(), false).unwrap();
    this.line.slice(0, this.moveIndex).forEach(usi => pos.play(parseUsi(pretendItsUsi(usi))!));
    return pos;
  };

  expectedMove = () => this.line[this.moveIndex];

  lastMove = () => this.line[this.moveIndex - 1];

  isOver = () => this.moveIndex >= this.line.length - 1;
}
