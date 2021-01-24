import { DailyBest } from './interfaces';
import { storedJsonProp } from 'common/storage';

const defaultBest = () => ({
  score: 0,
  at: Date.now()
})

const store = storedJsonProp<DailyBest>('storm.dailyBest', defaultBest);

export const get = (): DailyBest => {
  const v = store();
  return v.at > Date.now() - 1000 * 3600 * 12 ? v : defaultBest();
}

export const set = (n: number) => {
  const current = get();
  store({
    score: n > current.score ? n : current.score,
    prev: n >= current.score ? current.score : current.prev,
    at: Date.now()
  });
}
