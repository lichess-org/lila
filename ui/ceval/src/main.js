module.exports = {
  ctrl: require('./ctrl'),
  view: require('./view'),
  winningChances: require('./winningChances.js'),
  pv2san: require('./pv2san'),
  isEvalBetter: function(a, b) {
    return !b || a.depth > b.depth || (a.depth === b.depth && a.nodes > b.nodes);
  }
};

// stop when another tab starts. Listen only once here,
// as the ctrl can be instanciated several times.
// gotta do the click on the toggle to have it visually change.
lichess.storage.make('ceval.pool.start').listen(function(e) {
  var toggle = document.getElementById('analyse-toggle-ceval');
  if (toggle && toggle.checked) {
    console.log('Another tab runs the engine, closing this one.');
    toggle.click();
  }
});
