const config = {
  // all times in seconds
  clock: {
    initial: 5 * 60,
    // initial: 3 * 60,
    // initial: 30,
    malus: 15
  },
  combo: {
    levels: [
      // [combo threshold, time bonus]
      [0, 0],
      [3, 5],
      [6, 10],
      [9, 15],
      [12, 30]
    ]
    // levels: [
    //   [0, 0],
    //   [5, 5],
    //   [15, 10],
    //   [30, 15],
    //   [50, 30]
    // ]
  }
};

export default config;
