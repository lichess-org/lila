var util = require('./util');
var text = require('../text');
var pairings = require('./pairings');
var results = require('./results');

module.exports = function(ctrl) {
  return [
    util.title(ctrl),
    text.view(ctrl),
    results(ctrl),
    pairings(ctrl)
  ];
};
