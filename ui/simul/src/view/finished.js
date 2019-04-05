var m = require('mithril');
var util = require('./util');
var pairings = require('./pairings');
var results = require('./results');
var arbiter = require('./arbiter');

module.exports = function(ctrl) {
  return [
    util.arbiterOption(ctrl),
    util.exportGames(ctrl),
    m('div.box__top', [
      util.title(ctrl),
      m('div.box__top__actions', m('div.finished', ctrl.trans('finished')))
    ]),
    results(ctrl),
    arbiter(ctrl),
    pairings(ctrl)
  ];
};
