module.exports = function(old, cfg) {

  var data = cfg;

  if (old.userTv) data.userTv = old.userTv;

  if (data.clock) {
    data.clock.showTenths = data.pref.clockTenths;
    data.clock.showBar = data.pref.clockBar;
  }

  return data;
};
