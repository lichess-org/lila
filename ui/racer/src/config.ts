import { RacerConfig } from './interfaces';

const config: RacerConfig = {
  clock: {
    // initial: 99 * 60,
    // initial: 1 * 60,
    initial: 120,
    malus: 0,
  },
  combo: {
    levels: [
      [0, 0],
      [5, 1],
      [12, 2],
      [20, 3],
      [30, 4],
    ],
  },
};

export default config;
