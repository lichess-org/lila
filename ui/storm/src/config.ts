import { StormConfig } from './interfaces';

const config: StormConfig = {
  // all times in seconds
  clock: {
    initial: 3 * 60,
    // initial: 10,
    malus: 10,
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
  minFirstMoveTime: 400,
};

export default config;
