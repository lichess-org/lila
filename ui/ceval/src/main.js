module.exports = {
  ctrl: require('./ctrl'),
  view: require('./view'),
  winningChances: require('./winningChances.js'),
  pv2san: require('./pv2san'),
  isEvalBetter: function(a, b) {
    return !b || a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
  }
};

// stop when another tab starts. Listen only once.
lichess.storage.make('ceval.pool.start').listen(function() {
  var toggle = document.getElementById('analyse-toggle-ceval');
  if (toggle && toggle.checked) {
    console.log('ceval.pool.start, closing ceval');
    $('#analyse-toggle-ceval').click();
  }
});
