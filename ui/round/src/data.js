var merge = require('lodash-node/modern/objects/merge');

module.exports = function(cfg) {

  var data = {
  };

  merge(data, cfg);

  if (data.clock) {
    data.clock.showTenths = data.pref.clockTenths;
    data.clock.showBar = data.pref.clockBar;
  }

  return data;
};
