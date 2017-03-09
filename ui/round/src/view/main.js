var game = require('game').game;
var perf = require('game').perf;
var plyStep = require('../round').plyStep;
var renderTable = require('./table');
var renderPromotion = require('../promotion').view;
var chessground = require('../ground').render;
var fenRead = require('chessground/fen').read;
var mod = require('game').view.mod;
var util = require('../util');
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
  e.preventDefault();
  if (e.deltaY > 0) keyboard.next(ctrl);
  else if (e.deltaY < 0) keyboard.prev(ctrl);
  m.redraw();
  return false;
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
    }, chessground(ctrl)),
    renderPromotion(ctrl)
  ]);
}

function blindBoard(ctrl) {
  return m('div.lichess_board_blind', [
    m('div.textual', {
      config: function(el, isUpdate) {
        if (!isUpdate) blind.init(el, ctrl);
      }
    }),
    chessground(ctrl)
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
    cgState = ctrl.chessground && ctrl.chessground.state,
    material, score;
  var topColor = d[ctrl.vm.flip ? 'player' : 'opponent'].color;
  var bottomColor = d[ctrl.vm.flip ? 'opponent' : 'player'].color;
  if (d.pref.showCaptured) {
    var pieces = cgState ? cgState.pieces : fenRead(plyStep(ctrl.data, ctrl.vm.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
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
        ctrl.keyboardMove ? keyboardMove.view(ctrl.keyboardMove) : null
      ]),
      blursAndHolds(ctrl)
    ])
  ];
};
