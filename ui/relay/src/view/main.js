var m = require('mithril');
var util = require('chessground').util;
var games = require('./games');
var status = require('game').status;

function statusName(ctrl) {
  if (ctrl.data.status == 30) return ctrl.trans('finished');
  else return ctrl.data.games.filter(function(g) {
    return g.status < status.ids.mate;
  }).length + '/' + ctrl.data.games.length;
}

module.exports = function(ctrl) {

  return m('div', {
    class: 'content_box no_padding relay_box relay_show',
    'data-icon': 'n'
  }, [
    m('div.title_tag', statusName(ctrl)),
    m('h1', ctrl.data.name),
    (ctrl.data.content && ctrl.data.content.long) ? m('div.description', {
      config: function(el, isUpdate) {
        if (!isUpdate) {
          var $desc = $('#relay_description');
          $(el).html($desc.html());
          $desc.remove();
        }
      }
    }) : null,
    games(ctrl.data.games)
  ]);
};
