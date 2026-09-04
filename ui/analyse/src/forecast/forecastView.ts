import { h, type VNode } from 'snabbdom';

import { playable } from 'lib/game';
import { fixCrazySan } from 'lib/game/chess';
import { bind, spinnerVdom as spinner, snabIcon } from 'lib/view';

import type AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../pgnExport';
import type ForecastCtrl from './forecastCtrl';
import type { ForecastStep } from './interfaces';

function onMyTurn(fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  const firstNode = cNodes[0];
  if (!firstNode) return undefined;
  const fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return undefined;
  const lines = fcs.filter(fc => fc.length > 1);
  return h(
    'button.on-my-turn.button.text',
    {
      hook: bind('click', () => fctrl.playAndSave(firstNode)),
    },
    [
      snabIcon('checkmark'),
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
              hook: bind(
                'click',
                () =>
                  ctrl.userJump(
                    fctrl.showForecast(
                      ctrl.variantKey,
                      (playable(ctrl.data) && ctrl.initialPath) || '',
                      ctrl.tree,
                      nodes,
                    ),
                  ),
                ctrl.redraw,
              ),
            },
            [
              snabIcon('playTriangle'),
              h(
                'button.del',
                {
                  hook: bind('click', _ => fctrl.removeIndex(i), ctrl.redraw),
                  attrs: { 'aria-label': i18n.site.delete, type: 'button' },
                },
                [snabIcon('x')],
              ),
              h('sans', renderNodesHtml(nodes)),
            ],
          ),
        ),
      ),
      h(
        'button.add.text',
        {
          class: { enabled: isCandidate },
          hook: bind('click', () => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw),
        },
        [
          snabIcon(isCandidate ? 'plusButton' : 'infoCircle'),
          isCandidate
            ? h('span', [h('span', i18n.site.addCurrentVariation), h('sans', renderNodesHtml(cNodes))])
            : h('span', i18n.site.playVariationToCreateConditionalPremoves),
        ],
      ),
    ]),
    fctrl.onMyTurn() ? onMyTurn(fctrl, cNodes) : null,
  ]);
}
