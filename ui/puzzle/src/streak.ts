import { StoredJsonProp, storedJsonProp } from 'common/storage';
import { PuzzleData, Puzzle, PuzzleId, PuzzleGame } from './interfaces';

interface Current {
  puzzle: Puzzle;
  game: PuzzleGame;
}

interface StreakData {
  ids: PuzzleId[];
  index: number;
  skip: boolean;
  current: Current;
}

export default class PuzzleStreak {
  data: StreakData;
  fail = false;
  store: StoredJsonProp<StreakData | null>;

  constructor(data: PuzzleData) {
    this.store = storedJsonProp<StreakData | null>(`puzzle.streak.${data.user?.id || 'anon'}`, () => null);
    this.data = this.store() || {
      ids: data.streak!.split(' '),
      index: 0,
      skip: true,
      current: {
        puzzle: data.puzzle,
        game: data.game,
      },
    };
  }

  onComplete = (win: boolean, current?: Current) => {
    if (win) {
      if (this.nextId()) {
        this.data.index++;
        if (current)
          this.data.current = {
            puzzle: current.puzzle,
            game: current.game,
          };
        this.store(this.data);
      } else {
        this.store(null);
        lichess.reload();
      }
    } else {
      this.fail = true;
      this.store(null);
    }
  };

  nextId = () => this.data.ids[this.data.index + 1];

  skip = (): void => {
    this.data.skip = false;
    this.store(this.data);
  };
}
