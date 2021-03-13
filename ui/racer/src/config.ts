import { RacerConfig } from './interfaces';

const config: RacerConfig = {
  // all times in seconds
  clock: {
    // initial: 99 * 60,
    // initial: 1 * 60,
    initial: 60,
    malus: 5,
  },
  combo: {
    levels: [
      [0, 0],
      [5, 3],
      [12, 5],
      [20, 7],
      [30, 10],
    ],
  },
};

export default config;
