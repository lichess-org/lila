var m = require('mithril');

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
    treeView.render(ctrl, concealOf),
    // renderResult(ctrl)
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
          renderAnalyse(ctrl, concealOf),
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
