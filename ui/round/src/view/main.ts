import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plyStep } from '../round';
import { renderTable } from './table';
import * as promotion from '../promotion';
import { render as renderGround } from '../ground';
import { read as fenRead } from 'chessground/fen';
import * as util from '../util';
import * as keyboard from '../keyboard';
import crazyView from '../crazy/crazyView';
import { render as keyboardMove } from '../keyboardMove';
import RoundController from '../ctrl';
import { MaterialDiff, MaterialDiffSide } from '../interfaces';

function renderMaterial(material: MaterialDiffSide, score: number, checks?: number) {
  const children: VNode[] = [];
  let role: string, i: number;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (i = 0; i < material[role]; i++) content.push(h('mono-piece.' + role));
      children.push(h('tomb', content));
    }
  }
  if (checks) for (i = 0; i < checks; i++) children.push(h('tomb', h('mono-piece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.cemetery', children);
}

function wheel(ctrl: RoundController, e: WheelEvent): boolean {
  if (ctrl.isPlaying()) return true;
  e.preventDefault();
  if (e.deltaY > 0) keyboard.next(ctrl);
  else if (e.deltaY < 0) keyboard.prev(ctrl);
  ctrl.redraw();
  return false;
}

const emptyMaterialDiff: MaterialDiff = {
  white: {},
  black: {}
};

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
    cgState = ctrl.chessground && ctrl.chessground.state,
    topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
    bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color;
  let material: MaterialDiff, score: number = 0;
  if (d.pref.showCaptured) {
    let pieces = cgState ? cgState.pieces : fenRead(plyStep(ctrl.data, ctrl.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;
  return ctrl.blind ? ctrl.blind.render(ctrl) : h('div.round.cg-512', [
    h('div.lichess_game.gotomove.variant_' + d.game.variant.key + (ctrl.data.pref.blindfold ? '.blindfold' : ''), {
      hook: {
        insert: () => window.lichess.pubsub.emit('content_loaded')()
      }
    }, [
      h('div.lichess_board_wrap', [
        h('div.lichess_board.' + d.game.variant.key, {
          hook: util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e))
        }, [renderGround(ctrl)]),
        promotion.view(ctrl)
      ]),
      h('div.lichess_ground', [
        crazyView(ctrl, topColor, 'top') || renderMaterial(material[topColor], -score, d.player.checks),
        renderTable(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || renderMaterial(material[bottomColor], score, d.opponent.checks)
      ])
    ]),
    h('div.underboard', [
      h('div.center', {
        hook: {
          insert: vnode => {
            if (ctrl.opts.crosstableEl) {
              const el = (vnode.elm as HTMLElement);
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
