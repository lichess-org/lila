var m = require('mithril');
var util = require('./util');
var text = require('../text');
var pairings = require('./pairings');
var results = require('./results');
var arbiter = require('./arbiter');

module.exports = function(ctrl) {
  return [
    util.arbiterOption(ctrl),
    util.hostTv(ctrl),
    util.title(ctrl),
    text.view(ctrl),
    results(ctrl),
    arbiter(ctrl),
    pairings(ctrl)
  ];
};
