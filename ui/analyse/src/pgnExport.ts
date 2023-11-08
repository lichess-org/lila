import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom';
import { initialFen, fixCrazySan } from 'chess';
import { MaybeVNodes } from 'common/snabbdom';
import { plyToTurn } from './util';

interface PgnNode {
  ply: Ply;
  san?: San;
}

const plyPrefix = (node: Tree.Node): string =>
  `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '. ' : '... '}`;

function renderNodesTxt(node: Tree.Node, forcePly: boolean): string {
  if (node.children.length === 0) return '';

  let s = '';
  const first = node.children[0];
  if (forcePly || first.ply % 2 === 1) s += plyPrefix(first);
  s += fixCrazySan(first.san!);

  for (let i = 1; i < node.children.length; i++) {
    const child = node.children[i];
    s += ` (${plyPrefix(child)}${fixCrazySan(child.san!)}`;
    const variation = renderNodesTxt(child, false);
    if (variation) s += ' ' + variation;
    s += ')';
  }

  const mainline = renderNodesTxt(first, node.children.length > 1);
  if (mainline) s += ' ' + mainline;

  return s;
}

export function renderFullTxt(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game;
  let txt = renderNodesTxt(ctrl.tree.root, true);
  const tags: Array<[string, string]> = [];
  if (g.variant.key !== 'standard') tags.push(['Variant', g.variant.name]);
  if (g.initialFen && g.initialFen !== initialFen) tags.push(['FEN', g.initialFen]);
  if (tags.length) txt = tags.map(t => '[' + t[0] + ' "' + t[1] + '"]').join('\n') + '\n\n' + txt;
  return txt;
}

export function renderNodesHtml(nodes: PgnNode[]): MaybeVNodes {
  if (!nodes[0]) return [];
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return [];
  const tags: MaybeVNodes = [];
  if (nodes[0].ply % 2 === 0) tags.push(h('index', Math.floor((nodes[0].ply + 1) / 2) + '...'));
  nodes.forEach(node => {
    if (node.ply === 0) return;
    if (node.ply % 2 === 1) tags.push(h('index', (node.ply + 1) / 2 + '.'));
    tags.push(h('san', fixCrazySan(node.san!)));
  });
  return tags;
}

export function renderVariationPgn(nodeList: Tree.Node[]): string {
  const filteredNodeList = nodeList.filter(node => node.san);
  if (filteredNodeList.length === 0) {
    return '';
  }

  let variationPgn = '';

  const first = filteredNodeList[0];
  variationPgn += `${plyPrefix(first)}${first.san} `;

  for (let i = 1; i < filteredNodeList.length; i++) {
    const node = filteredNodeList[i];
    if (node.ply % 2 === 1) {
      variationPgn += plyToTurn(node.ply) + '. ';
    }

    variationPgn += fixCrazySan(node.san!) + ' ';
  }

  return variationPgn;
}
