var k = Mousetrap;
var control = require('./control');
var m = require('mithril');

function preventing(f) {
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

var i18nLoaded = false;

module.exports = {
  bind: function(ctrl) {
    k.bind(['left', 'k'], preventing(function() {
      control.prev(ctrl);
      m.redraw();
    }));
    k.bind(['shift+left', 'shift+k'], preventing(function() {
      control.exitVariation(ctrl);
      m.redraw();
    }));
    k.bind(['right', 'j'], preventing(function() {
      if (!ctrl.fork.proceed()) control.next(ctrl);
      m.redraw();
    }));
    k.bind(['shift+right', 'shift+j'], preventing(function() {
      control.enterVariation(ctrl);
      m.redraw();
    }));
    k.bind(['up', '0'], preventing(function() {
      if (!ctrl.fork.prev()) control.first(ctrl);
      m.redraw();
    }));
    k.bind(['down', '$'], preventing(function() {
      if (!ctrl.fork.next()) control.last(ctrl);
      m.redraw();
    }));
    k.bind('shift+c', preventing(function() {
      ctrl.vm.comments = !ctrl.vm.comments;
      ctrl.autoScroll();
      m.redraw();
    }));
    k.bind('esc', ctrl.chessground.cancelMove);
    k.bind('f', preventing(ctrl.flip));
    k.bind('?', preventing(function() {
      ctrl.vm.keyboardHelp = !ctrl.vm.keyboardHelp;
      m.redraw();
    }));
    k.bind('l', preventing(ctrl.toggleCeval));
    k.bind('a', preventing(function() {
      ctrl.toggleAutoShapes(!ctrl.vm.showAutoShapes());
      m.redraw();
    }));
    k.bind('x', preventing(ctrl.toggleThreatMode));
    k.bind('e', preventing(function() {
      ctrl.explorer.toggle();
      m.redraw();
    }));
    k.bind('space', preventing(function() {
      if (ctrl.ceval.enabled()) ctrl.playBestMove();
      else ctrl.toggleCeval();
    }));
    if (ctrl.study) {
      k.bind('c', preventing(function() {
        $('.study_buttons a.comment').click();
      }));
      k.bind('s', preventing(function() {
        $('.study_buttons a.glyph').click();
      }));
    }
  },
  view: function(ctrl) {

    var trans = ctrl.trans;

    if (!i18nLoaded) {
      i18nLoaded = true;
      $.ajax({
        dataType: "json",
        url: '/analysis/keyboard-i18n',
        cache: true,
        success: function(i18n) {
          trans.merge(i18n);
          m.redraw();
        }
      });
    }

    var header = function(text) {
      return m('tr', m('th[colspan=2]', m('p', text)));
    };
    var row = function(keys, desc) {
      return m('tr', [
        m('td.keys', keys),
        m('td.desc', desc)
      ]);
    };
    var k = function(key) {
      return m('kbd', key);
    }
    var or = m('or', '/');

    return m('div.lichess_overboard.keyboard_help', {
      config: function(el, isUpdate) {
        if (!isUpdate) lichess.loadCss('/assets/stylesheets/keyboard.css');
      }
    }, [
      m('a.close.icon[data-icon=L]', {
        onclick: function() {
          ctrl.vm.keyboardHelp = false;
        }
      }),
      m('div.scrollable', [
        m('h2', trans('keyboardShortcuts')),
        m('table', m('tbody', [
          header('Navigate the move tree'),
          row([k('←'), or, k('→')], trans('keyMoveBackwardOrForward')),
          row([k('j'), or, k('k')], trans('keyMoveBackwardOrForward')),
          row([k('↑'), or, k('↓')], trans('keyGoToStartOrEnd')),
          row([k('0'), or, k('$')], trans('keyGoToStartOrEnd')),
          row([k('shift'), k('←'), or, k('shift'), k('→')], trans('keyEnterOrExitVariation')),
          row([k('shift'), k('j'), or, k('shift'), k('k')], trans('keyEnterOrExitVariation')),
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
          m('tr', m('td.mouse[colspan=2]', m('ul', [
            m('li', trans('youCanAlsoScrollOverTheBoardToMoveInTheGame')),
            m('li', trans('pressShiftPlusClickOrRightClickToDrawCirclesAndArrowsOnTheBoard'))
          ])))
        ])),
      ])
    ]);
  }
};
