import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ForecastCtrl, ForecastStep } from './interfaces';
import AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../pgnExport';
import { bind, dataIcon, spinner } from '../util';
import { fixCrazySan } from 'chess';

function onMyTurn(fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  var firstNode = cNodes[0];
  if (!firstNode) return;
  var fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  var lines = fcs.filter(function(fc) {
    return fc.length > 1;
  });
  return h('button.on-my-turn.add.button.text', {
    attrs: dataIcon('E'),
    hook: bind('click', _ => fctrl.playAndSave(firstNode))
  }, [
    h('span', h('strong', 'Play ' + fixCrazySan(cNodes[0].san))),
    lines.length ?
    h('span', 'and save ' + lines.length + ' premove line' + (lines.length > 1 ? 's' : '')) :
    h('span', 'No conditional premoves')
  ]);
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const afterPly = ctrl.tree.getCurrentNodesAfterPly(ctrl.nodeList, ctrl.mainline, ctrl.data.game.turns);
  return fctrl.truncate(afterPly.map(node => ({
    ply: node.ply,
    fen: node.fen,
    uci: node.uci!,
    san: node.san!,
    check: node.check
  })));
}

export default function(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): VNode {
  const cNodes = makeCnodes(ctrl, fctrl);
  const isCandidate = fctrl.isCandidate(cNodes);
  return h('div.forecast', {
    class: { loading: fctrl.loading() }
  }, [
    fctrl.loading() ? h('div.overlay', spinner()) : null,
    h('div.box', [
      h('div.top', 'Conditional premoves'),
      h('div.list', fctrl.list().map(function(nodes, i) {
        return h('div.entry.text', {
          attrs: dataIcon('G')
        }, [
          h('a.del', {
            hook: bind('click', e => {
              fctrl.removeIndex(i);
              e.stopPropagation();
            }, ctrl.redraw)
          }, 'x'),
          h('sans', renderNodesHtml(nodes))
        ])
      })),
      h('button.add.button.text', {
        class: { enabled: isCandidate },
        attrs: dataIcon(isCandidate ? 'O' : ""),
        hook: bind('click', _ => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw)
      }, isCandidate ? [
        h('span', 'Add current variation'),
        h('sans', renderNodesHtml(cNodes))
      ] : [
        h('span', 'Play a variation to create'),
        h('span', 'conditional premoves')
      ])
    ]),
    fctrl.onMyTurn ? onMyTurn(fctrl, cNodes) : null
  ]);
}
