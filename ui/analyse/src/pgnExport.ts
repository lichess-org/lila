import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom';
import { initialFen } from 'chess';
import { MaybeVNodes } from './interfaces';
import { notationStyle } from 'common/notation';
import { ForecastStep } from './forecast/interfaces';

function plyPrefix(node: Tree.Node): string {
  return `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '. ' : '... '}`;
}

function renderNodesTxt(node: Tree.Node, forcePly: boolean): string {
  if (node.children.length === 0) return '';

  let s = '';
  const first = node.children[0];
  if (forcePly || first.ply % 2 === 1) s += plyPrefix(first);
  s += first.san;

  for (let i = 1; i < node.children.length; i++) {
    const child = node.children[i];
    s += ` (${plyPrefix(child)}${child.san}`;
    const variation = renderNodesTxt(child, false);
    if (variation) s += ' ' + variation;
    s += ')';
  }

  const mainline = renderNodesTxt(first, node.children.length > 1);
  if (mainline) s += ' ' + mainline;

  return s;
}

export function renderFullTxt(ctrl: AnalyseCtrl): string {
  var g = ctrl.data.game;
  var txt = renderNodesTxt(ctrl.tree.root, true);
  var tags: Array<[string, string]> = [];
  if (g.variant.key !== 'standard') tags.push(['Variant', g.variant.name]);
  if (g.initialFen && g.initialFen !== initialFen) tags.push(['FEN', g.initialFen]);
  if (tags.length)
    txt =
      tags
        .map(function (t) {
          return '[' + t[0] + ' "' + t[1] + '"]';
        })
        .join('\n') +
      '\n\n' +
      txt;
  return txt;
}

export function renderNodesHtml(nodes: ForecastStep[], notation: number): MaybeVNodes {
  if (!nodes[0]) return [];
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return [];
  const tags: MaybeVNodes = [];
  nodes.forEach(node => {
    if (node.ply === 0) return;
    tags.push(h('index', node.ply + '.'));
    tags.push(
      h(
        'san',
        notationStyle(notation)({
          san: node.san!,
          uci: node.uci!,
          fen: node.fen,
        })
      )
    );
  });
  return tags;
}
