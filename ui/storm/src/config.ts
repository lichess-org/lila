const config = {
  clock: {
    initial: 3 * 60 * 1000,
    // initial: 30 * 1000,
    malus: 15 * 1000,
    bonus: 1 * 1000
  },
  combo: {
    levels: [
      [0, 0],
      [5, 5],
      [15, 10],
      [30, 15],
      [50, 30]
    ]
  }
};

export default config;
