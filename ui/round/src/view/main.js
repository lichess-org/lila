var game = require('game').game;
var perf = require('game').perf;
var chessground = require('chessground');
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;
var mod = require('game').view.mod;
var partial = require('chessground').util.partial;
var button = require('./button');
var blind = require('../blind');
var keyboard = require('../keyboard');
var crazyDrag = require('../crazyDrag');
var m = require('mithril');

function materialTag(role) {
  return {
    tag: 'mono-piece',
    attrs: {
      class: role
    }
  };
}

function crazyPocketTag(role, color) {
  return {
    tag: 'div',
    attrs: {
      class: 'no-square'
    },
    children: [{
      tag: 'piece',
      attrs: {
        class: role + ' ' + color
      }
    }]
  };
}

function renderCrazyPocket(ctrl, color, position) {
  if (!ctrl.data.crazyhouse) return;
  var pocket = ctrl.data.crazyhouse.pockets[color === 'white' ? 0 : 1];
  var oKeys = Object.keys(pocket)
  var crowded = oKeys.length > 4;
  return m('div', {
      class: 'pocket ' + position + (oKeys.length > 4 ? ' crowded' : ''),
      config: position === 'bottom' ? function(el, isUpdate, context) {
        if (isUpdate) return;
        var onstart = partial(crazyDrag, ctrl);
        el.addEventListener('mousedown', onstart);
        context.onunload = function() {
          el.removeEventListener('mousedown', onstart);
        };
      } : null
    },
    oKeys.map(function(role) {
      var pieces = [];
      for (var i = 0; i < pocket[role]; i++) pieces.push(crazyPocketTag(role, color));
      return m('div', {
        class: 'role',
        'data-role': role,
        'data-color': color,
      }, pieces);
    })
  );
}

function renderMaterial(ctrl, material, checks) {
  var children = [];
  for (var role in material) {
    var piece = materialTag(role);
    var count = material[role];
    var content;
    if (count === 1) content = piece;
    else {
      content = [];
      for (var i = 0; i < count; i++) content.push(piece);
    }
    children.push(m('tomb', content));
  }
  for (var i = 0; i < checks; i++) {
    children.push(m('tomb', m('mono-piece.king[title=Check]')));
  }
  return m('div.cemetery', children);
}

function renderBerserk(ctrl, color, position) {
  if (ctrl.data.game.turns > 1 || !game.playable(ctrl.data)) return;
  if (!ctrl.vm.goneBerserk[color]) return;
  return m('div', {
    class: 'berserk_alert ' + position,
    'data-icon': '`'
  });
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
      }, 800);
    }
  });
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    m('div', {
      class: 'lichess_board ' + ctrl.data.game.variant.key + (ctrl.data.pref.blindfold ? ' blindfold' : ''),
      config: function(el, isUpdate) {
        if (!isUpdate) el.addEventListener('wheel', function(e) {
          return wheel(ctrl, e);
        });
      }
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

var emptyMaterialDiff = {
  white: [],
  black: []
};

function blursAndHolds(ctrl) {
  var stuff = [];
  ['blursOf', 'holdOf'].forEach(function(f) {
    ['opponent', 'player'].forEach(function(p) {
      var r = mod[f](ctrl, ctrl.data[p]);
      if (r) stuff.push(r);
    });
  });
  if (stuff.length) return m('div.blurs', stuff);
}

module.exports = function(ctrl) {
  var material = ctrl.data.pref.showCaptured ? chessground.board.getMaterialDiff(ctrl.chessground.data) : emptyMaterialDiff;
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
          renderBerserk(ctrl, ctrl.data.opponent.color, 'top'),
          renderCrazyPocket(ctrl, ctrl.data.opponent.color, 'top') || renderMaterial(ctrl, material[ctrl.data.opponent.color], ctrl.data.player.checks),
          renderTable(ctrl),
          renderCrazyPocket(ctrl, ctrl.data.player.color, 'bottom') || renderMaterial(ctrl, material[ctrl.data.player.color], ctrl.data.opponent.checks),
          renderBerserk(ctrl, ctrl.data.player.color, 'bottom'))
      ])
    ]),
    m('div.underboard', [
      m('div.center', ctrl.chessground.data.premovable.current ? m('div.premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel')) : null),
      blursAndHolds(ctrl)
    ])
  ];
};
