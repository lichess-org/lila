var m = require('mithril');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');
var arbiter = require('./arbiter');

module.exports = function(ctrl) {
  return [
    util.arbiterOption(ctrl),
    m('div.top_right.finished', ctrl.trans('finished')),
    util.title(ctrl),
    results(ctrl),
    arbiter(ctrl),
    pairings(ctrl)
  ];
};
