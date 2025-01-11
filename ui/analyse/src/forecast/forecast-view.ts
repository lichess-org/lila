import { type MaybeVNodes, bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import { i18n, i18nFormat, i18nPluralSame } from 'i18n';
import { makeNotationLine, notationsWithColor } from 'shogi/notation';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { ForecastCtrl, ForecastStep } from './interfaces';

function onMyTurn(fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  const firstNode = cNodes[0];
  if (!firstNode) return;
  const fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  const lines = fcs.filter(fc => fc.length > 1);
  const moveNotation = firstNode.notation || i18n('move');

  return h(
    'button.on-my-turn.button.text',
    {
      attrs: dataIcon('E'),
      hook: bind('click', _ => fctrl.playAndSave(firstNode)),
    },
    [
      h('span', [
        h('strong', i18nFormat('playX', moveNotation)),
        lines.length
          ? h('span', i18nPluralSame('andSaveNbPremoveLines', lines.length))
          : h('span', i18nFormat('noConditionalPremoves')),
      ]),
    ],
  );
}

function parentNode(ctrl: AnalyseCtrl, ply: number): Tree.Node {
  const properPly = ply - 1 - ctrl.mainline[0].ply;
  return ctrl.mainline[properPly];
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const afterPly = ctrl.tree.getCurrentNodesAfterPly(
    ctrl.nodeList,
    ctrl.mainline,
    ctrl.data.game.plies,
  );
  return fctrl.truncate(
    afterPly.map(node => ({
      ply: node.ply,
      sfen: node.sfen,
      usi: node.usi!,
      notation: node.notation!,
      check: node.check,
    })),
  );
}

function renderNodesHtml(nodes: ForecastStep[]): MaybeVNodes {
  if (!nodes[0]) return [];
  if (!nodes[0].usi) nodes = nodes.slice(1);
  if (!nodes[0]) return [];
  const tags: MaybeVNodes = [],
    addColorIcon = notationsWithColor();

  nodes.forEach((node, index) => {
    const colorIcon = addColorIcon ? `.color-icon.${node.ply % 2 ? 'gote' : 'sente'}` : '';
    tags.push(h('index', `${index + 1}.`));
    tags.push(h(`move-notation${colorIcon}`, node.notation));
  });
  return tags;
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
        h('div.top', i18n('conditionalPremoves')),
        h(
          'div.list',
          fctrl.list().map((nodes, i) => {
            const par = parentNode(ctrl, nodes[0].ply),
              notations = makeNotationLine(
                par.sfen,
                ctrl.data.game.variant.key,
                nodes.map(n => n.usi),
                par.usi,
              );
            notations.forEach((n, i) => {
              nodes[i].notation = n;
            });
            return h(
              'div.entry.text',
              {
                attrs: dataIcon('G'),
              },
              [
                h('button.del', {
                  hook: bind('click', _ => fctrl.removeIndex(i), ctrl.redraw),
                  attrs: { 'data-icon': 'L', type: 'button' },
                }),
                h('moves-notation', renderNodesHtml(nodes)),
              ],
            );
          }),
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
                  h('span', i18n('addCurrentVariation')),
                  h('moves-notation', renderNodesHtml(cNodes)),
                ])
              : h('span', i18n('playVariationToCreateConditionalPremoves')),
          ],
        ),
      ]),
      fctrl.onMyTurn ? onMyTurn(fctrl, cNodes) : null,
    ],
  );
}
