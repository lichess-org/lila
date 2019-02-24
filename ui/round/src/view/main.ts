import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { playable } from 'game';
import { plyStep } from '../round';
import { renderTable } from './table';
import * as promotion from '../promotion';
import { render as renderGround } from '../ground';
import { read as fenRead } from 'chessground/fen';
import * as util from '../util';
import * as keyboard from '../keyboard';
import crazyView from '../crazy/crazyView';
// import { render as keyboardMove } from '../keyboardMove';
import renderExpiration from './expiration';
import RoundController from '../ctrl';
import { Position, MaterialDiff, MaterialDiffSide, CheckCount } from '../interfaces';

function renderMaterial(material: MaterialDiffSide, score: number, position: Position, checks?: number) {
  const children: VNode[] = [];
  let role: string, i: number;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (i = 0; i < material[role]; i++) content.push(h('mpiece.' + role));
      children.push(h('div', content));
    }
  }
  if (checks) for (i = 0; i < checks; i++) children.push(h('div', h('mpiece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.material.material-' + position, children);
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

  const checks: CheckCount = (d.player.checks || d.opponent.checks) ?
    util.countChecks(ctrl.data.steps, ctrl.ply) :
    util.noChecks;

  const expiration = playable(ctrl.data) && renderExpiration(ctrl);

  return ctrl.nvui ? ctrl.nvui.render(ctrl) : h('div.round__app', [
    h('div.round__app__board.main-board.variant_' + d.game.variant.key + (ctrl.data.pref.blindfold ? '.blindfold' : ''), {
      class: { 'with-expiration': !!expiration },
      hook: util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e))
    }, [
      renderGround(ctrl),
      promotion.view(ctrl)
    ]),
    crazyView(ctrl, topColor, 'top') || renderMaterial(material[topColor], -score, 'top', checks[topColor]),
    ...renderTable(ctrl, expiration),
    crazyView(ctrl, bottomColor, 'bottom') || renderMaterial(material[bottomColor], score, 'bottom', checks[bottomColor])
  ])
  // h('div.underboard', [
  //   h('div.center', {
  //     hook: {
  //       insert: vnode => {
  //         if (ctrl.opts.crosstableEl) {
  //           const el = (vnode.elm as HTMLElement);
  //           el.insertBefore(ctrl.opts.crosstableEl, el.firstChild);
  //         }
  //       }
  //     }
  //   }, [
  //     ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null
  //   ])
  // ])
};
