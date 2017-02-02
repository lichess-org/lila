module.exports = {
  ctrl: require('./ctrl'),
  view: require('./view'),
  winningChances: require('./winningChances.js'),
  pv2san: require('./pv2san'),
  isEvalBetter: function(a, b) {
    return !b || a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
  }
};
