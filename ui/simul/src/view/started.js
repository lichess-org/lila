var m = require('mithril');
var util = require('./util');
var text = require('../text');
var pairings = require('./pairings');
var results = require('./results');
var arbiter = require('./arbiter');

module.exports = function(ctrl) {
  return [
    m('div.box__top', [
      util.title(ctrl),
      m('div.box__top__actions', [
        util.hostTv(ctrl),
        util.arbiterOption(ctrl)
      ])
    ]),
    text.view(ctrl),
    results(ctrl),
    arbiter(ctrl),
    pairings(ctrl)
  ];
};
