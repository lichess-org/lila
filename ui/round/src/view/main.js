var game = require('game').game;
var perf = require('game').perf;
var chessground = require('chessground');
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;
var mod = require('game').view.mod;
var partial = require('chessground').util.partial;
var button = require('./button');
var blind = require('../blind');
var keyboard = require('../replay/keyboard');
var m = require('mithril');

function renderMaterial(ctrl, material) {
  var children = [];
  for (var role in material) {
    var piece = m('div.mono-piece.' + role);
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

function wheel(ctrl, e) {
  if (game.isPlayerPlaying(ctrl.data)) return true;
  if (e.deltaY > 0) keyboard.next(ctrl);
  else if (e.deltaY < 0) keyboard.prev(ctrl);
  m.redraw();
  e.preventDefault();
  return false;
}

function renderVariantReminder(ctrl) {
  if (!game.isPlayerPlaying(ctrl.data) || ctrl.data.game.speed !== 'correspondence') return;
  if (ctrl.data.game.variant.key === 'standard') return;
  var icon = perf.icons[ctrl.data.game.perf];
  if (!icon) return;
  return m('div', {
    class: 'variant_reminder is',
    'data-icon': icon,
    config: function(el, isUpdate) {
      if (!isUpdate) setTimeout(function() {
        el.classList.add('gone');
        setTimeout(function() {
          el.remove();
        }, 600);
      }, 500);
    }
  });
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    m('div.lichess_board.' + ctrl.data.game.variant.key, {
      config: function(el, isUpdate) {
        if (!isUpdate) el.addEventListener('wheel', function(e) {
          return wheel(ctrl, e);
        });
      },
      onclick: ctrl.data.player.spectator ? toggleDontTouch : null
    }, chessground.view(ctrl.chessground)),
    renderPromotion(ctrl),
    renderVariantReminder(ctrl)
  ]);
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

var dontTouch = m.prop(false);

function toggleDontTouch() {
  dontTouch(!dontTouch());
}

module.exports = function(ctrl) {
  var material = ctrl.data.pref.showCaptured ? chessground.board.getMaterialDiff(ctrl.chessground.data) : false;
  return [
    m('div.top', [
      m('div.lichess_game', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          $('body').trigger('lichess.content_loaded');
        }
      }, [
        ctrl.data.blind ? blindBoard(ctrl) : visualBoard(ctrl),
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
        }, ctrl.trans('youAreViewingThisGameAsASpectator')) : null
      ]),
      m('div.blurs', [
        [ctrl.data.opponent, ctrl.data.player].map(partial(mod.blursOf, ctrl)), [ctrl.data.opponent, ctrl.data.player].map(partial(mod.holdOf, ctrl))
      ])
    ])
  ];
};
