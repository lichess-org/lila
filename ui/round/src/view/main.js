var m = require('mithril');
var round = require('../round');
var chessground = require('chessground');
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;
var renderUser = require('./user');
var partial = require('chessground').util.partial;
var button = require('./button');

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

function blursOf(ctrl, player) {
  if (player.blurs) return m('p', [
    renderUser(ctrl, player, player.color),
    ' ' + player.blurs.nb + '/' + round.nbMoves(ctrl.data, player.color) + ' blurs = ',
    m('strong', player.blurs.percent + '%')
  ]);
}

function holdOf(ctrl, player) {
  var h = player.hold;
  if (h) return m('p', [
    renderUser(ctrl, player, player.color),
    ' hold alert',
    m('br'),
    'ply=' + h.ply + ', mean=' + h.mean + ' ms, SD=' + h.sd
  ]);
}

var dontTouch = m.prop(false);

function toggleDontTouch() {
  dontTouch(!dontTouch());
}

module.exports = function(ctrl) {
  var material = ctrl.data.pref.showCaptured ? chessground.board.getMaterialDiff(ctrl.chessground.data) : false;
  return [
    m('div.top', [
      m('div.lichess_game.cg-512', {
        config: function(el, isUpdate, context) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blindMode ? m('div#lichess_board_blind') : null,
        m('div.lichess_board_wrap', ctrl.data.blindMode ? null : [
          m('div.lichess_board.' + ctrl.data.game.variant.key, {
            onclick: ctrl.data.player.spectator ? toggleDontTouch : null
          }, chessground.view(ctrl.chessground)),
          renderPromotion(ctrl)
        ]),
        m('div.lichess_ground',
          material ? renderMaterial(ctrl, material[ctrl.data.opponent.color]) : null,
          renderTable(ctrl),
          material ? renderMaterial(ctrl, material[ctrl.data.player.color]) : null)
      ])
    ]),
    m('div.underboard', [
      m('div.center', [
        ctrl.chessground.data.premovable.current ? m('div.premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel')) : null,
        dontTouch() ? m('div.dont_touch', {
          onclick: toggleDontTouch
        }, ctrl.trans('youAreViewingThisGameAsASpectator')) : null,
        button.flip(ctrl),
        button.replayAndAnalyse(ctrl)
      ]),
      m('div.right', [
        [ctrl.data.opponent, ctrl.data.player].map(partial(blursOf, ctrl)), [ctrl.data.opponent, ctrl.data.player].map(partial(holdOf, ctrl))
      ])
    ])
  ];
};
