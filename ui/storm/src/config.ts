const config = {
  // all times in seconds
  clock: {
    initial: 3 * 60,
    // initial: 3 * 60,
    // initial: 30,
    malus: 5
  },
  combo: {
    levels: [
      // [combo threshold, time bonus]
      [0, 0],
      [5, 5],
      [12, 10],
      [20, 15],
      [30, 20]
      // [0, 0],
      // [2, 10],
      // [4, 10],
      // [6, 10],
      // [8, 30]
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
