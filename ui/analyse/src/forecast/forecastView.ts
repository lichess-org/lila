import { h, type VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind, dataIcon } from 'common/snabbdom';
import type { ForecastStep } from './interfaces';
import type AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../pgnExport';
import { spinnerVdom as spinner } from 'common/spinner';
import { fixCrazySan } from 'chess';
import { findCurrentPath } from '../treeView/common';
import type ForecastCtrl from './forecastCtrl';

function onMyTurn(fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
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
      attrs: dataIcon(licon.Checkmark),
      hook: bind('click', _ => fctrl.playAndSave(firstNode)),
    },
    [
      h('span', [
        h('strong', i18n.site.playX(fixCrazySan(cNodes[0].san))),
        lines.length
          ? h('span', i18n.site.andSaveNbPremoveLines(lines.length))
          : h('span', i18n.site.noConditionalPremoves),
      ]),
    ],
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
    })),
  );
}

export default function (ctrl: AnalyseCtrl, fctrl: ForecastCtrl): VNode {
  const cNodes = makeCnodes(ctrl, fctrl);
  const isCandidate = fctrl.isCandidate(cNodes);
  return h('div.forecast', { class: { loading: fctrl.loading() } }, [
    fctrl.loading() ? h('div.overlay', spinner()) : null,
    h('div.box', [
      h('div.top', i18n.site.conditionalPremoves),
      h(
        'div.list',
        fctrl.forecasts().map((nodes, i) =>
          h(
            'button.entry.text',
            {
              attrs: dataIcon(licon.PlayTriangle),
              hook: bind(
                'click',
                () => {
                  const path = fctrl.showForecast(findCurrentPath(ctrl) || '', ctrl.tree, nodes);
                  ctrl.userJump(path);
                },
                ctrl.redraw,
              ),
            },
            [
              h('button.del', {
                hook: bind('click', _ => fctrl.removeIndex(i), ctrl.redraw),
                attrs: { 'data-icon': licon.X, type: 'button' },
              }),
              h('sans', renderNodesHtml(nodes)),
            ],
          ),
        ),
      ),
      h(
        'button.add.text',
        {
          class: { enabled: isCandidate },
          attrs: dataIcon(isCandidate ? licon.PlusButton : licon.InfoCircle),
          hook: bind('click', _ => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw),
        },
        [
          isCandidate
            ? h('span', [h('span', i18n.site.addCurrentVariation), h('sans', renderNodesHtml(cNodes))])
            : h('span', i18n.site.playVariationToCreateConditionalPremoves),
        ],
      ),
    ]),
    fctrl.onMyTurn() ? onMyTurn(fctrl, cNodes) : null,
  ]);
}
