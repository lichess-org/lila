import AnalyseCtrl from './ctrl';
import {h} from 'snabbdom';
import {initialFen, fixCrazySan} from 'chess';
import {MaybeVNodes} from './interfaces';

interface PgnNode {
  ply: Ply;
  san?: San;
  children: PgnNode[];
}

function renderNode(node?: PgnNode): string {
  if (!node || !node.san || node.ply === 0) return '';
  let s = '';
  if (node.ply % 2 === 1)
    s += ((node.ply + 1) / 2) + '. ';
  return s + fixCrazySan(node.san) + ' ';
}

function rootNodePrefix(node: PgnNode): string {
  return node.ply % 2 === 1 ? '' : Math.floor((node.ply + 1) / 2) + '... ';
}

function renderChildren(children: PgnNode[], isRoot: boolean = false): string {
  if (!children.length)
    return '';

  let s = '';
  const firstChild = children[0];

  if (isRoot)
    s += rootNodePrefix(firstChild);
  s += renderNode(firstChild);

  children.slice(1).forEach(child => {
    s += '(';
    if (isRoot)
      s += rootNodePrefix(child);
    s += renderNode(child);
    s += renderChildren(child.children);
    s = s.trim();
    s += ') ';
  });

  s += renderChildren(firstChild.children);

  return s;
}

export function renderFullTxt(ctrl: AnalyseCtrl): string {
  var g = ctrl.data.game;
  var txt = renderChildren(ctrl.tree.root.children, true);
  var tags: Array<[string, string]> = [];
  if (g.variant.key !== 'standard')
    tags.push(['Variant', g.variant.name]);
  if (g.initialFen && g.initialFen !== initialFen)
    tags.push(['FEN', g.initialFen]);
  if (tags.length)
    txt = tags.map(function(t) {
      return '[' + t[0] + ' "' + t[1] + '"]';
    }).join('\n') + '\n\n' + txt;
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
    if (node.ply % 2 === 1) tags.push(h('index', ((node.ply + 1) / 2) + '.'));
    tags.push(h('san', fixCrazySan(node.san!)));
  });
  return tags;
}
