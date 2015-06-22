module.exports = function(old, cfg) {

  var data = cfg;

  if (data.clock) {
    data.clock.showTenths = data.pref.clockTenths;
    data.clock.showBar = data.pref.clockBar;
  }

  if (data.correspondence)
    data.correspondence.showBar = data.pref.clockBar;

  if (data.game.variant.key === 'horde')
    data.pref.showCaptured = false;

  return data;
};
