import { game } from 'game';
import round = require('../round');
import table = require('./table');
import promotion = require('../promotion');
import ground = require('../ground');
import { read as fenRead } from 'chessground/fen';
import util = require('../util');
import blind = require('../blind');
import keyboard = require('../keyboard');
import crazyView from '../crazy/crazyView';
import { render as keyboardMove } from '../keyboardMove';

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function renderMaterial(material, checks, score) {
  var children: VNode[] = [];
  if (score || score === 0)
    children.push(h('score', score > 0 ? '+' + score : score));
  for (var role in material) {
    const content: VNode[] = [];
    for (var i = 0; i < material[role]; i++) content.push(h('mono-piece.' + role));
    children.push(h('tomb', content));
  }
  for (var i = 0; i < checks; i++) {
    children.push(h('tomb', h('mono-piece.king')));
  }
  return h('div.cemetery', children);
}

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

export function main(ctrl: any): VNode {
  const d = ctrl.data,
  cgState = ctrl.chessground && ctrl.chessground.state,
  topColor = d[ctrl.vm.flip ? 'player' : 'opponent'].color,
  bottomColor = d[ctrl.vm.flip ? 'opponent' : 'player'].color;
  let material, score;
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
        crazyView(ctrl, topColor, 'top') || renderMaterial(material[topColor], d.player.checks, undefined),
        table.render(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || renderMaterial(material[bottomColor], d.opponent.checks, score)
      ])
    ]),
    h('div.underboard', [
      h('div.center', {
        hook: {
          insert: vnode => {
            if (ctrl.opts.crosstableEl) {
              const el = (vnode.elm as HTMLElement)
                el.insertBefore(ctrl.opts.crosstableEl, el.firstChild);
            }
          }
        }
      }, [
        ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null
      ])
    ])
  ]);
};
