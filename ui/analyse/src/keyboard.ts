import * as control from './control';
import AnalyseCtrl from './ctrl';
import { bind as bindEvent } from './util';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function preventing(f: () => void): (e: MouseEvent) => void {
  return function(e) {
    if (e.preventDefault) {
      e.preventDefault();
    } else {
      // internet explorer
      e.returnValue = false;
    }
    f();
  };
}

let i18nLoaded = false;

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
  kbd.bind('esc', function() {
    ctrl.chessground.cancelMove();
  });

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
  kbd.bind('space', preventing(function() {
    if (ctrl.ceval.enabled()) ctrl.playBestMove();
    else ctrl.toggleCeval();
  }));
  if (ctrl.study) {
    kbd.bind('c', preventing(function() {
      $('.study_buttons a.comment').each(function(this: HTMLElement) {
        this.click();
      });
    }));
    kbd.bind('s', preventing(function() {
      $('.study_buttons a.glyph').each(function(this: HTMLElement) {
        this.click();
      });
    }));
  }
}

export function view(ctrl: AnalyseCtrl): VNode {

  const trans = ctrl.trans;

  if (!i18nLoaded) {
    i18nLoaded = true;
    $.ajax({
      dataType: "json",
      url: '/analysis/keyboard-i18n',
      cache: true,
      success: function(i18n) {
        trans.merge(i18n);
        ctrl.redraw();
      }
    });
  }

  function header(text: string) {
    return h('tr', h('th', {
      attrs: { colspan: 2 }
    }, [h('p', text)]));
  };
  function row(keys: VNode[], desc: string) {
    return h('tr', [
      h('td.keys', keys),
      h('td.desc', desc)
    ]);
  };
  function k(key: string) { return h('kbd', key); }
  function or() { return h('or', '/'); }

  return h('div.lichess_overboard.keyboard_help', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/keyboard.css')
    }
  }, [
    h('a.close.icon', {
      attrs: { 'data-icon': 'L' },
      hook: bindEvent('click', () => ctrl.keyboardHelp = false, ctrl.redraw)
    }),
    h('div.scrollable', [
      h('h2', trans('keyboardShortcuts')),
      h('table', h('tbody', [
        header('Navigate the move tree'),
        row([k('←'), or(), k('→')], trans('keyMoveBackwardOrForward')),
        row([k('j'), or(), k('k')], trans('keyMoveBackwardOrForward')),
        row([k('↑'), or(), k('↓')], trans('keyGoToStartOrEnd')),
        row([k('0'), or(), k('$')], trans('keyGoToStartOrEnd')),
        row([k('shift'), k('←'), or(), k('shift'), k('→')], trans('keyEnterOrExitVariation')),
        row([k('shift'), k('j'), or(), k('shift'), k('k')], trans('keyEnterOrExitVariation')),
        header('Analysis options'),
        row([k('l')], 'Local computer analysis'),
        row([k('a')], 'Computer arrows'),
        row([k('space')], 'Play computer best move'),
        row([k('x')], 'Show threat'),
        row([k('e')], 'Opening/endgame explorer'),
        row([k('f')], trans('flipBoard')),
        row([k('/')], 'Focus chat'),
        row([k('shift'), k('c')], trans('keyShowOrHideComments')),
        row([k('?')], 'Show this help dialog'),
        ctrl.study ? [
          header('Study actions'),
          row([k('c')], 'Comment this position'),
          row([k('s')], 'Annotate with symbols')
        ] : null,
        header('Mouse tricks'),
        h('tr', h('td.mouse', {
          attrs: { colspan: 2 }
        }, [h('ul', [
          h('li', trans('youCanAlsoScrollOverTheBoardToMoveInTheGame')),
          h('li', trans('analysisShapesHowTo'))
        ])]))
      ])),
    ])
  ]);
}
