import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plyStep } from '../round';
import renderTable from './table';
import * as promotion from '../promotion';
import { render as renderGround } from '../ground';
import { read as fenRead } from 'draughtsground/fen';
import * as util from '../util';
import * as blind from '../blind';
import * as keyboard from '../keyboard';
import crazyView from '../crazy/crazyView';
import { render as keyboardMove } from '../keyboardMove';
import RoundController from '../ctrl';
import * as cg from 'draughtsground/types';

function renderMaterial(material: cg.MaterialDiffSide, score: number, checks?: number) {
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

const emptyMaterialDiff: cg.MaterialDiff = {
  white: {},
  black: {}
};

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
    cgState = ctrl.draughtsground && ctrl.draughtsground.state,
    topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
    bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color,
    noAssistance = d.simul && d.simul.noAssistance;
  let material: cg.MaterialDiff, score: number = 0;
  if (d.pref.showCaptured && !noAssistance) {
    var pieces = cgState ? cgState.pieces : fenRead(plyStep(ctrl.data, ctrl.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;
  return h('div.round.cg-512', [
    h('div.lidraughts_game.gotomove.variant_' + d.game.variant.key + (ctrl.data.pref.blindfold ? '.blindfold' : ''), {
      hook: {
        insert: () => window.lidraughts.pubsub.emit('content_loaded')()
      }
    }, d.blind ? blind.view(ctrl) : [
      h('div.lidraughts_board_wrap', [
        h('div.lidraughts_board.' + d.game.variant.key, {
          hook: util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e))
        }, [renderGround(ctrl)]),
        promotion.view(ctrl)
      ]),
      h('div.lidraughts_ground', [
        crazyView(ctrl, topColor, 'top') || renderMaterial(material[topColor], -score),
        renderTable(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || renderMaterial(material[bottomColor], score)
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
