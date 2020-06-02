import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom'
import { initialFen } from 'draughts';
import { MaybeVNodes } from './interfaces';
import { ops as treeOps } from 'tree';
import { toggleCoordinates } from 'draughtsground/fen';

interface PdnNode {
  ply: Ply;
  displayPly?: Ply;
  san?: San;
  alg?: string;
}

function renderNodesTxt(nodes: PdnNode[]): string {
  if (!nodes[0]) return '';
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return '';
  var s = nodes[0].ply % 2 === 1 ? '' : Math.floor((nodes[0].ply + 1) / 2) + '... ';
  nodes.forEach(function (node, i) {
    if (node.ply === 0) return;
    if (node.ply % 2 === 1) s += ((node.ply + 1) / 2) + '. '
    else s += '';
    s += (node.alg ? node.alg.replace(':', 'x') : node.san!) + ((i + 9) % 8 === 0 ? '\n' : ' ');
  });
  return s.trim();
}

export function renderFullTxt(ctrl: AnalyseCtrl, fromNode?: boolean): string {
  var g = ctrl.data.game;
  var txt = renderNodesTxt(fromNode ? treeOps.mainlineNodeList(ctrl.node) : ctrl.tree.getNodeList(ctrl.path));
  var tags: Array<[string, string]> = [];
  if (g.variant.key !== 'standard' && g.variant.key !== 'fromPosition') {
    tags.push(g.variant.gameType ? ['GameType', g.variant.gameType] : ['Variant', g.variant.name]);
  }
  if (fromNode) {
    const fen = ctrl.tree.nodeAtPath(ctrl.path.slice(0, -2)).fen;
    tags.push(['FEN', toggleCoordinates(fen, ctrl.isAlgebraic())]);
  } else if (g.initialFen && g.initialFen !== initialFen)
    tags.push(['FEN', toggleCoordinates(g.initialFen, ctrl.isAlgebraic())]);
  if (tags.length)
    txt = tags.map(function (t) {
      return '[' + t[0] + ' "' + t[1] + '"]';
    }).join('\n') + '\n\n' + txt;
  return txt;
}

export function renderNodesHtml(nodes: PdnNode[]): MaybeVNodes {

  if (!nodes[0]) return [];
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return [];

  var lastIndex = -1;

  const tags: MaybeVNodes = [], startply = (nodes[0].displayPly ? nodes[0].displayPly : nodes[0].ply);
  if (startply && startply % 2 === 0) {
    lastIndex = Math.floor((startply + 1) / 2);
    tags.push(h('index', lastIndex + '...'));
  }

  nodes.forEach(node => {

    const dply = node.displayPly ? node.displayPly : node.ply;
    if (dply === 0) return;

    const cindex = (dply + 1) / 2;
    if (cindex !== lastIndex && dply % 2 === 1) {
      tags.push(h('index', cindex + '.'));
      lastIndex = cindex;
    }

    tags.push(h('san', node.san!));

  });

  return tags;

}
