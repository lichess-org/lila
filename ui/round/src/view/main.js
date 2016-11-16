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
var vn = require('mithril/render/vnode');

function materialTag(role) {
  return vn('mono-piece', undefined, {
    class: role
  });
}

function renderMaterial(ctrl, material, checks, score) {
  var children = [];
  if (score || score === 0)
    children.push(vn('score', undefined, undefined, undefined, score > 0 ? '+' + score : score));
  for (var role in material) {
    var piece = materialTag(role);
    var count = material[role];
    var content;
    if (count === 1) content = piece;
    else {
      content = [];
      for (var i = 0; i < count; i++) content.push(piece);
    }
    children.push(vn('tomb', undefined, undefined, content));
  }
  for (var i = 0; i < checks; i++) {
    children.push(m('tomb', m('mono-piece.king[title=Check]')));
  }
  return vn('div', undefined, {
    class: 'cemetery'
  }, children);
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
    oncreate: function(vnode) {
      setTimeout(function() {
        vnode.dom.classList.add('gone');
        setTimeout(function() {
          vnode.dom.remove();
        }, 600);
      }, 800);
    }
  });
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    vn('div', undefined, {
      class: 'lichess_board ' + ctrl.data.game.variant.key + (ctrl.data.pref.blindfold ? ' blindfold' : ''),
      oncreate: function(vnode) {
        vnode.dom.addEventListener('wheel', function(e) {
          return wheel(ctrl, e);
        });
      }
    }, [chessground.view(ctrl.chessground)]),
    renderPromotion(ctrl),
    renderVariantReminder(ctrl)
  ]);
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      oncreate: function(vnode) {
        blind.init(vnode.dom, ctrl);
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

module.exports = function(vnode) {
  var ctrl = vnode.state;
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
        oncreate: function() {
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
