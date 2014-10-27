var m = require('mithril');
var chessground = require('chessground');
var partial = require('chessground').util.partial;

function renderMovelist(ctrl) {
  return ctrl.game.moves;
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap',
    m('div.lichess_board.' + ctrl.data.game.variant.key,
      chessground.view(ctrl.chessground)));
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      config: function(el, isUpdate) {
        if (!isUpdate) blind.init(el, ctrl);
      }
    }),
    chessground.view(ctrl.chessground)
  ]);
}

module.exports = function(ctrl) {
  return [
    m('div.top', [
      m('div.lichess_game', {
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground',
          renderMovelist(ctrl))
      ])
    ]),
    m('div.underboard', [
      m('div.right', [
        // [ctrl.data.opponent, ctrl.data.player].map(partial(blursOf, ctrl)), [ctrl.data.opponent, ctrl.data.player].map(partial(holdOf, ctrl))
      ])
    ])
  ];
};
