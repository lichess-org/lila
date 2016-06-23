var m = require('mithril');
var chessground = require('chessground');

module.exports = function(ctrl) {
  var s = ctrl.stage;

  return m('div.lichess_game', [
    m('div.lichess_board_wrap', [
      m('div.lichess_board', {
        config: function(el, isUpdate) {
          if (!isUpdate) setTimeout(function() {
            el.classList.add('board-init');
          }, 300);
        }
      },
        chessground.view(ctrl.chessground))
    ]),
    m('div.lichess_ground', [
      m('h2', [
        s.num + ' ' + s.title
      ])
    ])
  ]);
};
