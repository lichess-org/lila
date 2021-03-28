import { PuzzleId } from './interfaces';

export default class PuzzleStreak {
  current: number = 0;
  skipAvailable: boolean = true;
  fail: boolean = false;

  constructor(readonly ids: PuzzleId[]) {}

  onComplete = (win: boolean) => {
    if (win) this.current++;
    else this.fail = true;
  };

  currentId = () => this.ids[this.current];

  skip = (): void => {
    this.skipAvailable = false;
  };
}
