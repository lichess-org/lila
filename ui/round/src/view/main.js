var game = require('game').game;
var perf = require('game').perf;
var chessground = require('chessground');
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;
var mod = require('game').view.mod;
var button = require('./button');
var blind = require('../blind');
var keyboard = require('../keyboard');
var crazyView = require('../crazy/crazyView');
var keyboardMove = require('../keyboardMove');
var m = require('mithril');

function materialTag(role) {
  return {
    tag: 'mono-piece',
    attrs: {
      class: role
    }
  };
}

function renderMaterial(ctrl, material, checks, score) {
  var children = [];
  if (score || score === 0)
    children.push(m('score', score > 0 ? '+' + score : score));
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
  var d = ctrl.data,
    cgData = ctrl.chessground.data,
    material, score;
  var topColor = d[ctrl.vm.flip ? 'player' : 'opponent'].color;
  var bottomColor = d[ctrl.vm.flip ? 'opponent' : 'player'].color;
  if (d.pref.showCaptured) {
    material = chessground.board.getMaterialDiff(cgData);
    score = chessground.board.getScore(cgData) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;
  return [
    m('div.top', [
      m('div', {
        class: 'lichess_game variant_' + d.game.variant.key,
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.pubsub.emit('content_loaded')();
        }
      }, [
        d.blind ? blindBoard(ctrl) : visualBoard(ctrl),
        m('div.lichess_ground', [
          crazyView.pocket(ctrl, topColor, 'top') || renderMaterial(ctrl, material[topColor], d.player.checks),
          renderTable(ctrl),
          crazyView.pocket(ctrl, bottomColor, 'bottom') || renderMaterial(ctrl, material[bottomColor], d.opponent.checks, score)
        ])
      ])
    ]),
    m('div.underboard', [
      m('div.center', [
        cgData.premovable.current || cgData.predroppable.current.key ? m('div.premove_alert', ctrl.trans('premoveEnabledClickAnywhereToCancel')) : null,
        ctrl.keyboardMove ? keyboardMove.view(ctrl.keyboardMove) : null,
      ]),
      blursAndHolds(ctrl)
    ])
  ];
};
