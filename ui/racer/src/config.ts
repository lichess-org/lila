import { Config } from 'puz/interfaces';

const config: Config = {
  // all times in seconds
  clock: {
    initial: 1 * 60,
    // initial: 10,
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
  timeToStart: 1000 * 60 * 2,
};

export default config;
