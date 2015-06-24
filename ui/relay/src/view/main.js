var m = require('mithril');
var util = require('chessground').util;
var games = require('./games');

function statusName(ctrl, s) {
  if (s == 30) return ctrl.trans('finished');
  else return ctrl.trans('started');
}

module.exports = function(ctrl) {

  return m('div', {
    class: 'content_box no_padding relay_box relay_show'
  }, [
    m('div.title_tag', statusName(ctrl, ctrl.data.status)),
    m('h1.text[data-icon=n]', ctrl.data.name),
    games(ctrl.data.games)
  ]);
};
