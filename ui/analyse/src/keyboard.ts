import * as control from './control';
import AnalyseCtrl from './ctrl';
import { spinner } from './util';
import { modal } from './modal';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

const preventing = (f: () => void) => (e: MouseEvent) => {
  e.preventDefault();
  f();
}

export function bind(ctrl: AnalyseCtrl): void {
  if (!window.Mousetrap) return;
  const kbd = window.Mousetrap;
  kbd.bind(['left', 'k'], preventing(function() {
    control.prev(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['shift+left', 'shift+k'], preventing(function() {
    control.exitVariation(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['right', 'j'], preventing(function() {
    if (!ctrl.fork.proceed()) control.next(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['shift+right', 'shift+j'], preventing(function() {
    control.enterVariation(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['up', '0'], preventing(function() {
    if (!ctrl.fork.prev()) control.first(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['down', '$'], preventing(function() {
    if (!ctrl.fork.next()) control.last(ctrl);
    ctrl.redraw();
  }));
  kbd.bind('shift+c', preventing(function() {
    ctrl.showComments = !ctrl.showComments;
    ctrl.autoScroll();
    ctrl.redraw();
  }));
  kbd.bind('shift+i', preventing(function() {
    ctrl.treeView.toggle();
    ctrl.redraw();
  }));

  if (ctrl.embed) return;

  kbd.bind('space', preventing(function() {
    const gb = ctrl.gamebookPlay();
    if (gb) gb.onSpace();
    else if (ctrl.studyPractice) return;
    else if (ctrl.ceval.enabled()) ctrl.playBestMove();
    else ctrl.toggleCeval();
  }));

  if (ctrl.studyPractice) return;

  kbd.bind('f', preventing(ctrl.flip));
  kbd.bind('?', preventing(function() {
    ctrl.keyboardHelp = !ctrl.keyboardHelp;
    ctrl.redraw();
  }));
  kbd.bind('l', preventing(ctrl.toggleCeval));
  kbd.bind('a', preventing(function() {
    ctrl.toggleAutoShapes(!ctrl.showAutoShapes());
    ctrl.redraw();
  }));
  kbd.bind('x', preventing(ctrl.toggleThreatMode));
  kbd.bind('e', preventing(function() {
    ctrl.toggleExplorer();
    ctrl.redraw();
  }));
  if (ctrl.study) {
    const keyToMousedown = (key: string, selector: string) => {
      kbd.bind(key, preventing(function() {
        $(selector).each(function(this: HTMLElement) {
          window.lichess.dispatchEvent(this, 'mousedown');
        });
      }));
    };
    keyToMousedown('c', '.study__buttons .comments');
    keyToMousedown('g', '.study__buttons .glyphs');
  }
}

export function view(ctrl: AnalyseCtrl): VNode {
  return modal({
    class: 'keyboard-help',
    onInsert(el: HTMLElement) {
      window.lichess.loadCssPath('analyse.keyboard')
      $(el).find('.scrollable').load('/analysis/help?study=' + (ctrl.study ? 1 : 0));
    },
    onClose() {
      ctrl.keyboardHelp = false;
      ctrl.redraw();
    },
    content: [
      h('div.scrollable', spinner())
    ]
  });
}
