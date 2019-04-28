var m = require('mithril');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');

module.exports = function(ctrl) {
  return [
    m('div.box__top', [
      util.title(ctrl),
      m('div.box__top__actions', m('div.finished', ctrl.trans('finished')))
    ]),
    util.simulText(ctrl.data),
    results(ctrl),
    pairings(ctrl)
  ];
};
