import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plyStep } from '../round';
import { renderTable } from './table';
import { render as renderGround } from '../ground';
import { read as fenRead } from 'draughtsground/fen';
import * as util from '../util';
import * as keyboard from '../keyboard';
import * as gridHacks from './gridHacks';
import { render as keyboardMove } from '../keyboardMove';
import RoundController from '../ctrl';
import * as cg from 'draughtsground/types';
import { Position } from '../interfaces';

function renderMaterial(material: cg.MaterialDiffSide, score: number, position: Position) {
  const children: VNode[] = [];
  let role: string, i: number;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (i = 0; i < material[role]; i++) content.push(h('mpiece.' + role));
      children.push(h('div', content));
    }
  }
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
    let pieces = cgState ? cgState.pieces : fenRead(plyStep(ctrl.data, ctrl.ply).fen);
    material = util.getMaterialDiff(pieces);
    score = util.getScore(pieces) * (bottomColor === 'white' ? 1 : -1);
  } else material = emptyMaterialDiff;

  return ctrl.nvui ? ctrl.nvui.render(ctrl) : h('div.round__app.variant-' + d.game.variant.key + '.is' + ctrl.data.game.variant.board.key, {
    class: { 'move-confirm': !!(ctrl.moveToSubmit || ctrl.dropToSubmit) },
    hook: util.onInsert(gridHacks.start)
  }, [
    h('div.round__app__board.main-board' + (ctrl.data.pref.blindfold ? '.blindfold' : ''), {
      hook: window.lidraughts.hasTouchEvents ? undefined :
        util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e), undefined, false)
    }, [
      renderGround(ctrl)
    ]),
    renderMaterial(material[topColor], -score, 'top'),
    ...renderTable(ctrl),
    renderMaterial(material[bottomColor], score, 'bottom'),
    ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null
  ])
};
