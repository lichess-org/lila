import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import draughtsground from './draughtsground';
import { render as treeView } from './tree';
import { view as cevalView } from 'ceval';
import * as control from '../control';
import feedbackView from './feedback';
import historyView from './history';
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
  return h('div.areplay', [
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

function visualBoard(ctrl: Controller) {
  return h('div.lidraughts_board_wrap', [
    h('div.lidraughts_board', {
      hook: bind('wheel', e => wheel(ctrl, e as WheelEvent))
    }, [
      draughtsground(ctrl),
      ctrl.promotion.view()
    ]),
    cevalView.renderGauge(ctrl)
  ]);
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

function buttons(ctrl: Controller) {
  return h('div.game_control', {
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
  const showCeval = false; //ctrl.vm.showComputer();
  if (cevalShown !== showCeval) {
    if (!cevalShown) ctrl.vm.autoScrollNow = true;
    cevalShown = showCeval;
  }
  return h('div#puzzle.cg-512', [
    h('div', {
      hook: {
        insert: _ => window.lidraughts.pubsub.emit('reset_zoom')()
      },
      class: { gauge_displayed: ctrl.showEvalGauge() }
    }, [
      h('div.lidraughts_game', {
        hook: {
          insert: _ => window.lidraughts.pubsub.emit('content_loaded')()
        }
      }, [
        visualBoard(ctrl),
        h('div.lidraughts_ground', [
          // we need the wrapping div here
          // so the siblings are only updated when ceval is added
          h('div', showCeval ? [
            cevalView.renderCeval(ctrl),
            cevalView.renderPvs(ctrl)
          ] : []),
          renderAnalyse(ctrl),
          feedbackView(ctrl),
          buttons(ctrl)
        ])
      ])
    ]),
    h('div.underboard', [
      h('div.center', [
        historyView(ctrl)
      ])
    ])
  ]);
};
