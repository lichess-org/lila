import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { ForecastCtrl, ForecastStep } from './interfaces';
import AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../notationExport';
import { bind, dataIcon, spinner } from '../util';
import { makeNotation } from 'common/notation';

function onMyTurn(ctrl: AnalyseCtrl, fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  var firstNode = cNodes[0];
  if (!firstNode) return;
  var fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  var lines = fcs.filter(function (fc) {
    return fc.length > 1;
  });
  const initialSfen = firstNode.sfen;
  const moveNotation = makeNotation(
    ctrl.data.pref.pieceNotation,
    initialSfen,
    ctrl.data.game.variant.key,
    cNodes[0].usi
  );
  return h(
    'button.on-my-turn.button.text',
    {
      attrs: dataIcon('E'),
      hook: bind('click', _ => fctrl.playAndSave(firstNode)),
    },
    [
      h('span', [
        h('strong', ctrl.trans('playX', moveNotation!)),
        lines.length
          ? h('span', ctrl.trans.plural('andSaveNbPremoveLines', lines.length))
          : h('span', ctrl.trans.noarg('noConditionalPremoves')),
      ]),
    ]
  );
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const afterPly = ctrl.tree.getCurrentNodesAfterPly(ctrl.nodeList, ctrl.mainline, ctrl.data.game.plies);
  return fctrl.truncate(
    afterPly.map(node => ({
      ply: node.ply,
      sfen: node.sfen,
      usi: node.usi!,
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
                attrs: dataIcon('G'),
              },
              [
                h(
                  'a.del',
                  {
                    hook: bind(
                      'click',
                      e => {
                        fctrl.removeIndex(i);
                        e.stopPropagation();
                      },
                      ctrl.redraw
                    ),
                  },
                  'x'
                ),
                h('moves-notation', renderNodesHtml(nodes, ctrl.data.pref.pieceNotation, ctrl.data.game.variant.key)),
              ]
            );
          })
        ),
        h(
          'button.add.text',
          {
            class: { enabled: isCandidate },
            attrs: dataIcon(isCandidate ? 'O' : ''),
            hook: bind('click', _ => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw),
          },
          [
            isCandidate
              ? h('span', [
                  h('span', ctrl.trans.noarg('addCurrentVariation')),
                  h(
                    'moves-notation',
                    renderNodesHtml(cNodes, ctrl.data.pref.pieceNotation, ctrl.data.game.variant.key)
                  ),
                ])
              : h('span', ctrl.trans.noarg('playVariationToCreateConditionalPremoves')),
          ]
        ),
      ]),
      fctrl.onMyTurn ? onMyTurn(ctrl, fctrl, cNodes) : null,
    ]
  );
}
