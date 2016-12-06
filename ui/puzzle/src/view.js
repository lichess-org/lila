var m = require('mithril');
var chessground = require('chessground');
var bindOnce = require('common').bindOnce;
var treeView = require('./treeView');
var control = require('./control');
var feedbackView = require('./feedbackView');

function renderOpeningBox(ctrl) {
  var opening = ctrl.tree.getOpening(ctrl.vm.nodeList);
  if (opening) return m('div', {
    class: 'opening_box',
    title: opening.eco + ' ' + opening.name
  }, [
    m('strong', opening.eco),
    ' ' + opening.name
  ]);
}

function renderAnalyse(ctrl) {
  return m('div.areplay', [
    renderOpeningBox(ctrl),
    treeView.render(ctrl),
    // renderResult(ctrl)
  ]);
}

function wheel(ctrl, e) {
  if (e.target.tagName !== 'PIECE' && e.target.tagName !== 'SQUARE' && !e.target.classList.contains('cg-board')) return;
  if (e.deltaY > 0) control.next(ctrl);
  else if (e.deltaY < 0) control.prev(ctrl);
  m.redraw();
  e.preventDefault();
  return false;
}

function visualBoard(ctrl) {
  return m('div.lichess_board_wrap', [
    m('div.lichess_board', {
      config: function(el, isUpdate) {
        if (!isUpdate) el.addEventListener('wheel', function(e) {
          return wheel(ctrl, e);
        });
      }
    }, [
      chessground.view(ctrl.ground)
    ]),
    // cevalView.renderGauge(ctrl)
  ]);
}

function dataAct(e) {
  return e.target.getAttribute('data-act') ||
    e.target.parentNode.getAttribute('data-act');
}

function jumpButton(icon, effect) {
  return {
    tag: 'button',
    attrs: {
      'data-act': effect,
      'data-icon': icon
    }
  };
}

var cachedButtons = (function() {
  return m('div.jumps', [
    jumpButton('W', 'first'),
    jumpButton('Y', 'prev'),
    jumpButton('X', 'next'),
    jumpButton('V', 'last')
  ])
})();

function buttons(ctrl) {
  return m('div.game_control', {
    config: bindOnce('mousedown', function(e) {
      var action = dataAct(e);
      if (action === 'prev') control.prev(ctrl);
      else if (action === 'next') control.next(ctrl);
      else if (action === 'first') control.first(ctrl);
      else if (action === 'last') control.last(ctrl);
      // else if (action === 'explorer') ctrl.explorer.toggle();
      // else if (action === 'menu') ctrl.actionMenu.toggle();
    })
  }, [
    cachedButtons
  ]);
}

var firstRender = true;

module.exports = function(ctrl) {
  return [
    m('div', {
      config: function(el, isUpdate) {
        if (firstRender) firstRender = false;
        else if (!isUpdate) lichess.pubsub.emit('reset_zoom')();
      }
    }, [
      m('div.lichess_game', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          lichess.pubsub.emit('content_loaded')();
        }
      }, [
        visualBoard(ctrl),
        m('div.lichess_ground', [
          // cevalView.renderCeval(ctrl),
          // cevalView.renderPvs(ctrl),
          renderAnalyse(ctrl),
          feedbackView(ctrl),
          buttons(ctrl)
        ])
      ])
    ]),
    m('div.underboard', [
      m('div.center', [
      ])
    ])
  ];
};
