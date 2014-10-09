var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;

function renderMaterial(ctrl, material) {
  var children = [];
  for (var role in material) {
    var piece = m('div.grave', m('div.mono-piece.' + role));
    var count = material[role];
    var content;
    if (count === 1) content = piece;
    else {
      content = [];
      for (var i = 0; i < count; i++) content.push(piece);
    }
    children.push(m('div.tomb', content));
  }
  return m('div.cemetery', children);
}

module.exports = function(ctrl) {
  var material = ctrl.data.pref.showCaptured ? chessground.board.getMaterialDiff(ctrl.chessground.data) : false;
  return m('div.lichess_game.cg-512', {
    config: function(el, isUpdate, context) {
      if (isUpdate) return;
      $('body').trigger('lichess.content_loaded');
    }
  }, [
    ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
    m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
      m('div.lichess_board.' + ctrl.data.game.variant.key, chessground.view(ctrl.chessground)),
      ctrl.chessground.data.premovable.current ? m('div#premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel')) : null,
      renderPromotion(ctrl)
    ]),
    m('div.lichess_ground',
      material ? renderMaterial(ctrl, material[ctrl.data.opponent.color]) : null,
      renderTable(ctrl),
      material ? renderMaterial(ctrl, material[ctrl.data.player.color]) : null)
  ]);
};
