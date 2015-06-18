module.exports = {
  game: require('./game'),
  status: require('./status'),
  view: {
    status: require('./view/status'),
    user: require('./view/user'),
    mod: require('./view/mod')
  },
  perf: {
    icons: {
      bullet: "T",
      blitz: ")",
      classical: "+",
      correspondence: ";",
      chess960: "'",
      kingOfTheHill: "(",
      threeCheck: ".",
      antichess: "@",
      atomic: ">",
      horde: "_"
    }
  }
};
