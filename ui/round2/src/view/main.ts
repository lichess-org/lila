import { game } from 'game';
import round = require('../round');
import table = require('./table');
import promotion = require('../promotion');
import ground = require('../ground');
import { read as fenRead } from 'chessground/fen';
import util = require('../util');
import blind = require('../blind');
import keyboard = require('../keyboard');
// var crazyView = require('../crazy/crazyView');
// var keyboardMove = require('../keyboardMove');

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

// function materialTag(role) {
//   return {
//     tag: 'mono-piece',
//     attrs: {
//       class: role
//     }
//   };
// }

// function renderMaterial(ctrl, material, checks, score) {
//   var children = [];
//   if (score || score === 0)
//     children.push(m('score', score > 0 ? '+' + score : score));
//   for (var role in material) {
//     var piece = materialTag(role);
//     var count = material[role];
//     var content;
//     if (count === 1) content = piece;
//     else {
//       content = [];
//       for (var i = 0; i < count; i++) content.push(piece);
//     }
//     children.push(m('tomb', content));
//   }
//   for (var i = 0; i < checks; i++) {
//     children.push(m('tomb', m('mono-piece.king[title=Check]')));
//   }
//   return m('div.cemetery', children);
// }

function wheel(ctrl, e) {
  if (game.isPlayerPlaying(ctrl.data)) return true;
  e.preventDefault();
  if (e.deltaY > 0) keyboard.next(ctrl);
  else if (e.deltaY < 0) keyboard.prev(ctrl);
  ctrl.redraw();
  return false;
}

function visualBoard(ctrl) {
  return h('div.lichess_board_wrap', [
    h('div', {
      class: {
        'lichess_board': true,
        [ctrl.data.game.variant.key]: true,
        'blindfold': ctrl.data.pref.blindfold
      },
      hook: util.bind('click', e => wheel(ctrl, e))
    }, [ground.render(ctrl)]),
    promotion.view(ctrl)
  ]);
}

function blindBoard(ctrl) {
  return h('div.lichess_board_blind', [
    h('div.textual', {
      hook: {
        insert: vnode => blind.init(vnode.elm, ctrl)
      }
    }, [ ground.render(ctrl) ])
  ]);
}

var emptyMaterialDiff = {
  white: [],
  black: []
};

// function blursAndHolds(ctrl) {
//   var stuff = [];
//   ['blursOf', 'holdOf'].forEach(function(f) {
//     ['opponent', 'player'].forEach(function(p) {
//       var r = game.view.mod[f](ctrl, ctrl.data[p]);
//       if (r) stuff.push(r);
//     });
//   });
//   if (stuff.length) return m('div.blurs', stuff);
// }

export function main(ctrl: any): VNode {
  var d = ctrl.data,
    cgState = ctrl.chessground && ctrl.chessground.state,
    material, score;
  // var topColor = d[ctrl.vm.flip ? 'player' : 'opponent'].color;
  var bottomColor = d[ctrl.vm.flip ? 'opponent' : 'player'].color;
  if (d.pref.showCaptured) {
    var pieces = cgState ? cgState.pieces : fenRead(round.plyStep(ctrl.data, ctrl.vm.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;
  return h('div.round.cg-512', [
    h('div.lichess_game.variant_' + d.game.variant.key, {
      hook: {
        insert: () => window.lichess.pubsub.emit('content_loaded')()
      }
    }, [
      d.blind ? blindBoard(ctrl) : visualBoard(ctrl),
      h('div.lichess_ground', [
        // crazyView.pocket(ctrl, topColor, 'top') || renderMaterial(ctrl, material[topColor], d.player.checks),
        table.render(ctrl),
        // crazyView.pocket(ctrl, bottomColor, 'bottom') || renderMaterial(ctrl, material[bottomColor], d.opponent.checks, score)
        ])
    ])
  ]);
  // m('div.underboard', [
  //   m('div.center', {
  //     config: function(el, isUpdate) {
  //       if (!isUpdate && ctrl.opts.crosstableEl) el.insertBefore(ctrl.opts.crosstableEl, el.firstChild);
  //     }
  //   }, ctrl.keyboardMove ? keyboardMove.view(ctrl.keyboardMove) : null),
  //   blursAndHolds(ctrl)
  // ])
  // ];
  };
