var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');

module.exports = function(ctrl) {
  return [
    m('div.title_tag', ctrl.trans('finished')),
    util.title(ctrl),
    util.games(ctrl.data.lastGames)
  ];
};
