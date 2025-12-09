import { prop } from 'lib';
import { storedJsonProp } from 'lib/storage';
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
  maxAge = 1000 * 3600;

  constructor(
    readonly theme: ThemeKey,
    readonly userId: string | undefined,
    readonly streak: boolean,
  ) {}

  default = () => ({
    theme: this.theme,
    rounds: [],
    at: Date.now(),
  });

  store = this.streak
    ? prop(this.default())
    : storedJsonProp<Store>(`puzzle.session.${this.userId || 'anon'}`, this.default);

  clear = () => this.update(s => ({ ...s, rounds: [] }));

  get = () => {
    const prev = this.store();
    return prev.theme === this.theme && prev.at > Date.now() - this.maxAge ? prev : this.default();
  };

  update = (f: (s: Store) => Store): Store => this.store(f(this.get()));

  complete = (id: string, result: boolean) =>
    this.update(s => {
      const i = s.rounds.findIndex(r => r.id === id);
      if (i === -1) {
        s.rounds.push({ id, result });
        if (s.rounds.length > this.maxSize) s.rounds.shift();
      } else s.rounds[i].result = result;
      s.at = Date.now();
      return s;
    });

  setRatingDiff = (id: string, ratingDiff: number) =>
    this.update(s => {
      s.rounds.forEach(r => {
        if (r.id === id) r.ratingDiff = ratingDiff;
      });
      return s;
    });

  isNew = (): boolean => this.store().rounds.length < 2;
}
