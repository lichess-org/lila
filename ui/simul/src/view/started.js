var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');

module.exports = function(ctrl) {
  return [
    util.title(ctrl),
    util.simulText(ctrl.data),
    results(ctrl),
    pairings(ctrl)
  ];
};
