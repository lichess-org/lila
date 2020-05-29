import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ForecastCtrl, ForecastStep } from './interfaces';
import AnalyseCtrl from '../ctrl';
import { renderNodesHtml } from '../pdnExport';
import { bind, dataIcon, spinner } from '../util';
import { read, write } from 'draughtsground/fen'
import { key2pos } from 'draughtsground/util'
import { calcCaptKey } from 'draughtsground/board'

function onMyTurn(ctrl: AnalyseCtrl, fctrl: ForecastCtrl, cNodes: ForecastStep[]): VNode | undefined {
  var firstNode = cNodes[0];
  if (!firstNode) return;
  var fcs = fctrl.findStartingWithNode(firstNode);
  if (!fcs.length) return;
  var lines = fcs.filter(function (fc) {
    return fc.length > 1;
  });
  return h('button.on-my-turn.button.text', {
    attrs: dataIcon('E'),
    hook: bind('click', _ => fctrl.playAndSave(firstNode))
  }, [
      h('span', [
        h('strong', ctrl.trans('playX', cNodes[0].san!)),
        lines.length ?
        h('span', ctrl.trans.plural('andSaveNbPremoveLines', lines.length)) :
        h('span', ctrl.trans.noarg('noConditionalPremoves'))
      ])
    ]);
}

function shortKey(key: string) {
  return key.slice(0, 1) === '0' ? key.slice(1) : key;
}

function makeCnodes(ctrl: AnalyseCtrl, fctrl: ForecastCtrl): ForecastStep[] {
  const withCurrent = ctrl.tree.getCurrentNodesAfterPly(ctrl.nodeList, ctrl.mainline, Math.max(0, ctrl.data.game.turns - 1)), 
    expandedNodes: ForecastStep[] = [];
  let afterCurrent: Tree.Node[] = [], skippedSteps = 0, currentFen;
  for (let n = 0; n < withCurrent.length; n++) {
    const node = withCurrent[n], nodePly = node.displayPly ? node.displayPly : node.ply;
    if (nodePly > ctrl.data.game.turns) {
      afterCurrent = withCurrent.slice(n);
      break;
    }
    currentFen = node.fen;
  }
  if (!afterCurrent) return expandedNodes;
  for (let node of fctrl.truncateNodes(afterCurrent)) {
    if (node.uci && node.uci.length >= 6 && currentFen) {
      let uci = node.uci, orig = uci.slice(0, 2);
      const pieces = read(currentFen), origPiece = pieces[orig];
      const boardSize = ctrl.data.game.variant.board.size;
      while (uci.length >= 4) {
        delete pieces[orig];
        const origPos = key2pos(orig, boardSize), dest = uci.slice(2, 4), destPos = key2pos(dest, boardSize),
          captKey = calcCaptKey(pieces, boardSize, origPos[0], origPos[1], destPos[0], destPos[1]);
        if (!captKey) break;
        const captPiece = pieces[captKey];
        pieces[captKey] = {
          role: captPiece.role === 'king' ? 'ghostking' : 'ghostman',
          color: captPiece.color
        };
        pieces[dest] = origPiece;

        skippedSteps++;
        if (skippedSteps > fctrl.skipSteps) {
          const done = uci.length === 4, fen = done ? node.fen : currentFen.slice(0, 2) + write(pieces);
          expandedNodes.push({
            ply: done ? node.ply : (node.ply - 1),
            displayPly: node.ply,
            fen: fen,
            uci: uci.slice(0, 4),
            san: shortKey(orig) + 'x' + shortKey(dest)
          });
        }

        uci = uci.slice(2);
        orig = dest;
      }
    } else {
      skippedSteps++;
      if (skippedSteps > fctrl.skipSteps)
        expandedNodes.push({
          ply: node.ply,
          displayPly: node.displayPly,
          fen: node.fen,
          uci: node.uci!,
          san: node.alg || node.san!
        });
    }
    if (expandedNodes.length) {
      currentFen = expandedNodes[expandedNodes.length - 1].fen;
    }
  }
  return expandedNodes;
}

export default function (ctrl: AnalyseCtrl, fctrl: ForecastCtrl): VNode {
  const cNodes = makeCnodes(ctrl, fctrl);
  const isCandidate = fctrl.isCandidate(cNodes);
  return h('div.forecast', {
    class: { loading: fctrl.loading() }
  }, [
      fctrl.loading() ? h('div.overlay', spinner()) : null,
      h('div.box', [
        h('div.top', ctrl.trans.noarg('conditionalPremoves')),
        h('div.list', fctrl.list().map(function (nodes, i) {
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
        h('button.add.text', {
          class: { enabled: isCandidate },
          attrs: dataIcon(isCandidate ? 'O' : ""),
          hook: bind('click', _ => fctrl.addNodes(makeCnodes(ctrl, fctrl)), ctrl.redraw)
        }, [
          isCandidate ? h('span', [
            h('span', ctrl.trans.noarg('addCurrentVariation')),
            h('sans', renderNodesHtml(cNodes))
          ]) :
          h('span', ctrl.trans.noarg('playVariationToCreateConditionalPremoves'))
        ])
      ]),
      fctrl.onMyTurn ? onMyTurn(ctrl, fctrl, cNodes) : null
    ]);
}
