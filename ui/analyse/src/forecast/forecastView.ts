import { h, VNode } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { ForecastCtrl, ForecastStep } from './interfaces';
import AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../pgnExport';
import { dataIcon, spinner } from '../util';
import { fixCrazySan } from 'chess';

function onMyTurn(ctrl: AnalyseCtrl, fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  const firstNode = cNodes[0];
  if (!firstNode) return;
  const fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  const lines = fcs.filter(function (fc) {
    return fc.length > 1;
  });
  return h(
    'button.on-my-turn.button.text',
    {
      attrs: dataIcon(''),
      hook: bind('click', _ => fctrl.playAndSave(firstNode)),
    },
    [
      h('span', [
        h('strong', ctrl.trans('playX', fixCrazySan(cNodes[0].san))),
        lines.length
          ? h('span', ctrl.trans.plural('andSaveNbPremoveLines', lines.length))
          : h('span', ctrl.trans.noarg('noConditionalPremoves')),
      ]),
    ]
  );
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const afterPly = ctrl.tree.getCurrentNodesAfterPly(ctrl.nodeList, ctrl.mainline, ctrl.data.game.turns);
  return fctrl.truncate(
    afterPly.map(node => ({
      ply: node.ply,
      fen: node.fen,
      uci: node.uci!,
      san: node.san!,
      check: node.check,
    }))
  );
}

export default function (ctrl: AnalyseCtrl, fctrl: ForecastCtrl): VNode {
  const cNodes = makeCnodes(ctrl, fctrl);
  const isCandidate = fctrl.isCandidate(cNodes);
  return h(
    'div.forecast',
    {
      class: { loading: fctrl.loading() },
    },
    [
      fctrl.loading() ? h('div.overlay', spinner()) : null,
      h('div.box', [
        h('div.top', ctrl.trans.noarg('conditionalPremoves')),
        h(
          'div.list',
          fctrl.list().map(function (nodes, i) {
            return h(
              'div.entry.text',
              {
                attrs: dataIcon(''),
              },
              [
                h('button.del', {
                  hook: bind('click', _ => fctrl.removeIndex(i), ctrl.redraw),
                  attrs: { 'data-icon': '', type: 'button' },
                }),
                h('sans', renderNodesHtml(nodes)),
              ]
            );
          })
        ),
        h(
          'button.add.text',
          {
            class: { enabled: isCandidate },
            attrs: dataIcon(isCandidate ? '' : ''),
            hook: bind('click', _ => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw),
          },
          [
            isCandidate
              ? h('span', [h('span', ctrl.trans.noarg('addCurrentVariation')), h('sans', renderNodesHtml(cNodes))])
              : h('span', ctrl.trans.noarg('playVariationToCreateConditionalPremoves')),
          ]
        ),
      ]),
      fctrl.onMyTurn ? onMyTurn(ctrl, fctrl, cNodes) : null,
    ]
  );
}
