function firstPly(d) {
  return d.steps[0].ply;
};

function lastPly(d) {
  return d.steps[d.steps.length - 1].ply;
};

function plyStep(d, ply) {
  return d.steps[ply - firstPly(d)];
};

module.exports = {
  merge: function(old, cfg) {
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
  },
  firstPly: firstPly,
  lastPly: lastPly,
  plyStep: plyStep
};
