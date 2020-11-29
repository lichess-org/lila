import {defined} from 'common';
import { storedJsonProp } from 'common/storage';

interface SessionRound {
  puzzleId: string;
  result?: boolean;
}
interface Store {
  theme: string;
  rounds: SessionRound[];
  at: number;
}

export default class PuzzleSession {

  maxSize = 50;
  maxAge = 1000 * 3600;

  constructor(readonly theme: string) {
  }

  default = () => ({
    theme: this.theme,
    rounds: [],
    at: Date.now()
  });

  store = storedJsonProp<Store>('puzzle.session', this.default);
  
  clear = () => this.update(s => ({ ...s, rounds: [] }));

  get = () => {
    const prev = this.store();
    return prev.theme == this.theme && prev.at > Date.now() - this.maxAge ? prev : this.default();
  }

  update = (f: (s: Store) => Store): Store => this.store(f(this.get()));

  start = (id: string) =>
    this.update(s => {
      s.rounds = this.addRound(s.rounds, id);
      s.at = Date.now();
      return s;
    });

  complete = (id: string, win: boolean) =>
    this.update(s => {
      s.rounds = this.addRound(s.rounds, id);
      s.rounds[0].result = win;
      s.at = Date.now();
      return s;
    });

  addRound = (rounds: SessionRound[], id: string) => {
    if (rounds[0]?.puzzleId != id) {
      if (rounds[0] && !defined(rounds[0].result)) rounds.shift();
      rounds.unshift({ puzzleId: id });
      if (rounds.length > this.maxSize) rounds.pop();
    }
    return rounds;
  }
}
