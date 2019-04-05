var m = require('mithril');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');

module.exports = function(ctrl) {
  return [
    m('div.finished', ctrl.trans('finished')),
    util.title(ctrl),
    results(ctrl),
    pairings(ctrl)
  ];
};
