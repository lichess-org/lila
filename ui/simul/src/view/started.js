var m = require('mithril');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');
var arbiter = require('./arbiter');

module.exports = function(ctrl) {
  return [
    util.arbiterOption(ctrl),
    util.hostTv(ctrl),
    util.title(ctrl),
    util.simulText(ctrl.data),
    results(ctrl),
    arbiter(ctrl),
    pairings(ctrl)
  ];
};
