import { type StoredJsonProp, storedJsonProp } from 'common/storage';
import type { ThemeKey } from './interfaces';

interface SessionRound {
  id: string;
  result?: boolean;
  ratingDiff?: number;
}
interface Store {
  theme: ThemeKey;
  rounds: SessionRound[];
  at: number;
}

export default class PuzzleSession {
  maxSize = 100;
  maxAge: number = 1000 * 3600;

  constructor(
    readonly theme: ThemeKey,
    readonly userId?: string,
  ) {}

  default = (): Store => ({
    theme: this.theme,
    rounds: [],
    at: Date.now(),
  });

  store: StoredJsonProp<Store> = storedJsonProp<Store>(
    `puzzle.session.${this.userId || 'anon'}`,
    this.default,
  );

  clear = (): Store => this.update(s => ({ ...s, rounds: [] }));

  get = (): Store => {
    const prev = this.store();
    return prev.theme == this.theme && prev.at > Date.now() - this.maxAge ? prev : this.default();
  };

  update = (f: (s: Store) => Store): Store => this.store(f(this.get()));

  complete = (id: string, result: boolean): Store =>
    this.update(s => {
      const i = s.rounds.findIndex(r => r.id == id);
      if (i == -1) {
        s.rounds.push({ id, result });
        if (s.rounds.length > this.maxSize) s.rounds.shift();
      } else s.rounds[i].result = result;
      s.at = Date.now();
      return s;
    });

  setRatingDiff = (id: string, ratingDiff: number): Store =>
    this.update(s => {
      s.rounds.forEach(r => {
        if (r.id == id) r.ratingDiff = ratingDiff;
      });
      return s;
    });

  isNew = (): boolean => this.store().rounds.length < 2;
}
