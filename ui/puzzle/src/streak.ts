import { StoredJsonProp, storedJsonProp } from 'common/storage';
import { PuzzleId } from './interfaces';

interface StreakData {
  ids: PuzzleId[];
  current: number;
  skip: boolean;
}

export default class PuzzleStreak {
  data: StreakData;
  fail: boolean = false;
  store: StoredJsonProp<StreakData | null>;

  constructor(ids: PuzzleId[], readonly userId: string | undefined) {
    this.store = storedJsonProp<StreakData | null>(`puzzle.streak.${this.userId || 'anon'}`, () => null);
    this.data = this.store() || {
      ids,
      current: 0,
      skip: true,
    };
  }

  onComplete = (win: boolean) => {
    if (win) this.data.current++;
    else {
      this.fail = true;
      this.store(null);
    }
  };

  currentId = () => this.data.ids[this.data.current];

  skip = (): void => {
    this.data.skip = false;
  };
}
