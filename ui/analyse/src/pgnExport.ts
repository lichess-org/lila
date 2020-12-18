import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom'
import { initialFen } from 'chess';
import { MaybeVNodes } from './interfaces';
import { notationStyle } from 'shogiutil/notation';
import { ForecastStep } from './forecast/interfaces';

interface PgnNode {
  ply: Ply;
  san?: San;
}

function renderNodesTxt(nodes: PgnNode[]): string {
  if (!nodes[0]) return '';
  if (!nodes[0].san) nodes = nodes.slice(1);
  if (!nodes[0]) return '';
  var s = nodes[0].ply % 2 === 1 ? '' : Math.floor((nodes[0].ply + 1) / 2) + '... ';
  nodes.forEach(function (node, i) {
    if (node.ply === 0) return;
    if (node.ply % 2 === 1) s += ((node.ply + 1) / 2) + '. '
    else s += '';
    s += node.san! + ((i + 9) % 8 === 0 ? '\n' : ' ');
  });
  return s.trim();
}

export function renderFullTxt(ctrl: AnalyseCtrl): string {
  var g = ctrl.data.game;
  var txt = renderNodesTxt(ctrl.tree.getNodeList(ctrl.path));
  var tags: Array<[string, string]> = [];
  if (g.variant.key !== 'standard')
    tags.push(['Variant', g.variant.name]);
  if (g.initialFen && g.initialFen !== initialFen)
    tags.push(['FEN', g.initialFen]);
  if (tags.length)
    txt = tags.map(function (t) {
      return '[' + t[0] + ' "' + t[1] + '"]';
    }).join('\n') + '\n\n' + txt;
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
    tags.push(h('san', notationStyle(notation)({
      san: node.san!,
      uci: node.uci!,
      fen: node.fen
    })));
  });
  return tags;
}
