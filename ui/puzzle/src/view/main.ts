import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import chessground from './chessground';
import { render as treeView } from './tree';
import { view as cevalView } from 'ceval';
import * as control from '../control';
import feedbackView from './feedback';
import historyView from './history';
import * as side from './side';
import { bind } from '../util';
import { Controller } from '../interfaces';

function renderOpeningBox(ctrl: Controller) {
  var opening = ctrl.getTree().getOpening(ctrl.vm.nodeList);
  if (opening) return h('div.opening_box', {
    attrs: { title: opening.eco + ' ' + opening.name }
  }, [
    h('strong', opening.eco),
    ' ' + opening.name
  ]);
}

function renderAnalyse(ctrl: Controller) {
  return h('div.puzzle__moves.areplay', [
    renderOpeningBox(ctrl),
    treeView(ctrl)
  ]);
}

function wheel(ctrl: Controller, e: WheelEvent) {
  const target = e.target as HTMLElement;
  if (target.tagName !== 'PIECE' && target.tagName !== 'SQUARE' && !target.classList.contains('cg-board')) return;
  e.preventDefault();
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  ctrl.redraw();
  return false;
}

function dataAct(e) {
  return e.target.getAttribute('data-act') || e.target.parentNode.getAttribute('data-act');
}

function jumpButton(icon, effect) {
  return h('button', {
    attrs: {
      'data-act': effect,
      'data-icon': icon
    }
  });
}

function controls(ctrl: Controller) {
  return h('div.puzzle__controls.analyse-controls', {
    hook: bind('mousedown', e => {
      const action = dataAct(e);
      if (action === 'prev') control.prev(ctrl);
      else if (action === 'next') control.next(ctrl);
      else if (action === 'first') control.first(ctrl);
      else if (action === 'last') control.last(ctrl);
    }, ctrl.redraw)
  }, [
    h('div.jumps', [
      jumpButton('W', 'first'),
      jumpButton('Y', 'prev'),
      jumpButton('X', 'next'),
      jumpButton('V', 'last')
    ])
  ]);
}

let cevalShown = false;

export default function(ctrl: Controller): VNode {
  const showCeval = ctrl.vm.showComputer();
  if (cevalShown !== showCeval) {
    if (!cevalShown) ctrl.vm.autoScrollNow = true;
    cevalShown = showCeval;
  }
  return h('main.puzzle', {
    class: {'gauge-on': ctrl.showEvalGauge()}
  }, [
    h('aside.puzzle__side', [
      side.puzzleBox(ctrl),
      side.userBox(ctrl)
    ]),
    cevalView.renderGauge(ctrl),
    h('div.puzzle__board.main-board' + (ctrl.pref.blindfold ? '.blindfold' : ''), {
      hook: bind('wheel', e => wheel(ctrl, e as WheelEvent))
    }, [
      chessground(ctrl),
      ctrl.promotion.view()
    ]),
    h('div.puzzle__tools', [
      // we need the wrapping div here
      // so the siblings are only updated when ceval is added
      h('div.ceval-wrap', {
        class: { none: !showCeval }
      }, showCeval ? [
        cevalView.renderCeval(ctrl),
        cevalView.renderPvs(ctrl)
      ] : []),
      renderAnalyse(ctrl),
      feedbackView(ctrl)
    ]),
    controls(ctrl),
    historyView(ctrl)
  ]);
}
