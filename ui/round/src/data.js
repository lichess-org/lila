module.exports = function(old, cfg) {

  var data = cfg;

  if (data.clock) {
    data.clock.showTenths = data.pref.clockTenths;
    data.clock.showBar = data.pref.clockBar;
  }

  if (cfg.game.moves) data.game.moves = data.game.moves.split(' ');

  return data;
};
