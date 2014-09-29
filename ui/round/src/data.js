var merge = require('lodash-node/modern/objects/merge');

module.exports = function(cfg) {

  var data = {
  };

  merge(data, cfg);

  return data;
};
