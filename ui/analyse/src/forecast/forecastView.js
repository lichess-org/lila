var m = require('mithril');
var partial = require('chessground').util.partial;
var pgnExport = require('../pgnExport');
var treePath = require('../tree/path');
var fixCrazySan = require('../util').fixCrazySan;

var onMyTurn = function(ctrl, fctrl, cNodes) {
  var firstNode = cNodes[0];
  if (!firstNode) return;
  var fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  var lines = fcs.filter(function(fc) {
    return fc.length > 1;
  });
  return m('button.on-my-turn', {
    class: 'add button text',
    'data-icon': 'E',
    onclick: partial(fctrl.playAndSave, firstNode)
  }, [
    m('span', m('strong', 'Play ' + fixCrazySan(cNodes[0].san))),
    lines.length ?
    m('span', 'and save ' + lines.length + ' premove line' + (lines.length > 1 ? 's' : '')) :
    m('span', 'No conditional premoves')
  ]);
};

module.exports = function(ctrl) {
  var fctrl = ctrl.forecast;
  var cNodes = fctrl.truncate(ctrl.tree.getCurrentNodesAfterPly(
    ctrl.vm.nodeList, ctrl.vm.mainline, ctrl.data.game.turns));
  var isCandidate = fctrl.isCandidate(cNodes);
  return m('div.forecast' + (fctrl.loading() ? '.loading' : ''), [
    fctrl.loading() ? m('div.overlay', m.trust(lichess.spinnerHtml)) : null,
    m('div.box', [
      m('div.top', 'Conditional premoves'),
      m('div.list', fctrl.list().map(function(nodes, i) {
        return m('div.entry', {
          'data-icon': 'G',
          class: 'text',
          onclick: function() {
            // #TODO #FIXME
            // needs the node ID to insert in the tree
            // but it's not stored by forecast
            return;
            var gamePath = treePath.fromNodeList(ctrl.vm.nodeList.slice(0, ctrl.data.game.turns + 1));
            ctrl.userJump(ctrl.tree.addNodes(
              nodes,
              gamePath));
          }
        }, [
          m('a', {
            class: 'del',
            onclick: function(e) {
              fctrl.removeIndex(i);
              e.stopPropagation();
            }
          }, 'x'),
          m('sans', m.trust(pgnExport.renderNodesHtml(nodes)))
        ])
      })),
      m('button', {
        class: 'add button text' + (isCandidate ? ' enabled' : ''),
        'data-icon': isCandidate ? 'O' : "",
        onclick: partial(fctrl.addNodes, cNodes)
      }, isCandidate ? [
        m('span', 'Add current variation'),
        m('span', m('sans', m.trust(pgnExport.renderNodesHtml(cNodes))))
      ] : [
        m('span', 'Play a variation to create'),
        m('span', 'conditional premoves')
      ])
    ]),
    fctrl.onMyTurn ? onMyTurn(ctrl, fctrl, cNodes) : null
  ]);
};
