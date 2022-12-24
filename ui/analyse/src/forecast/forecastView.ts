import { Notation, makeNotation, makeNotationLine, notationsWithColor } from 'common/notation';
import { MaybeVNodes, bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { ForecastCtrl, ForecastStep } from './interfaces';

function onMyTurn(ctrl: AnalyseCtrl, fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  var firstNode = cNodes[0];
  if (!firstNode) return;
  var fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  var lines = fcs.filter(function (fc) {
    return fc.length > 1;
  });
  const initialSfen = firstNode.sfen,
    moveNotation =
      firstNode.notation ||
      makeNotation(ctrl.data.pref.notation, initialSfen, ctrl.data.game.variant.key, cNodes[0].usi);
  return h(
    'button.on-my-turn.button.text',
    {
      attrs: dataIcon('E'),
      hook: bind('click', _ => fctrl.playAndSave(firstNode)),
    },
    [
      h('span', [
        h('strong', ctrl.trans('playX', moveNotation)),
        lines.length
          ? h('span', ctrl.trans.plural('andSaveNbPremoveLines', lines.length))
          : h('span', ctrl.trans.noarg('noConditionalPremoves')),
      ]),
    ]
  );
}

function parentNode(ctrl: AnalyseCtrl, ply: number): Tree.Node {
  return ctrl.mainline[ply - 1];
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const afterPly = ctrl.tree.getCurrentNodesAfterPly(ctrl.nodeList, ctrl.mainline, ctrl.data.game.plies);
  return fctrl.truncate(
    afterPly.map(node => ({
      ply: node.ply,
      sfen: node.sfen,
      usi: node.usi!,
      notation: node.notation!,
      check: node.check,
    }))
  );
}

function renderNodesHtml(nodes: ForecastStep[], notation: Notation): MaybeVNodes {
  if (!nodes[0]) return [];
  if (!nodes[0].usi) nodes = nodes.slice(1);
  if (!nodes[0]) return [];
  const tags: MaybeVNodes = [],
    addColorIcon = notationsWithColor.includes(notation);

  nodes.forEach((node, index) => {
    const colorIcon = addColorIcon ? ('.color-icon.' + (node.ply % 2) ? 'gote' : 'sente') : '';
    tags.push(h('index', index + 1 + '.'));
    tags.push(h('move-notation' + colorIcon, node.notation));
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
        h('div.top', ctrl.trans.noarg('conditionalPremoves')),
        h(
          'div.list',
          fctrl.list().map(function (nodes, i) {
            const par = parentNode(ctrl, nodes[0].ply),
              notations = makeNotationLine(
                ctrl.data.pref.notation,
                par.sfen,
                ctrl.data.game.variant.key,
                nodes.map(n => n.usi),
                par.usi
              );
            notations.map((n, i) => (nodes[i].notation = n));
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
                h('moves-notation', renderNodesHtml(nodes, ctrl.data.pref.notation)),
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
                  h('moves-notation', renderNodesHtml(cNodes, ctrl.data.pref.notation)),
                ])
              : h('span', ctrl.trans.noarg('playVariationToCreateConditionalPremoves')),
          ]
        ),
      ]),
      fctrl.onMyTurn ? onMyTurn(ctrl, fctrl, cNodes) : null,
    ]
  );
}
