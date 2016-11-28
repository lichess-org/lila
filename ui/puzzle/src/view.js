var m = require('mithril');
var chessground = require('chessground');
var treeView = require('./treeView');

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
          // buttons(ctrl)
        ])
      ])
    ]),
    m('div', {
      class: 'underboard'
    }, [
      m('div.center', 'under center')
    ])
  ];
};
