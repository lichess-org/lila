import { INITIAL_FEN } from 'chessops/fen';
import { h } from 'snabbdom';

import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { plyPrefix, renderNodesTxt } from 'lib/game/nodePGN';
import type { TreeNode } from 'lib/tree/types';
import { type MaybeVNodes } from 'lib/view';

import type AnalyseCtrl from './ctrl';
import type { Game } from './interfaces';

interface PgnNode {
  ply: Ply;
  san?: San;
}

function renderPgnTags(game: Game): string {
  let txt = '';
  const tags: Array<[string, string]> = [];
  if (game.variant.key !== 'standard') tags.push(['Variant', game.variant.name]);
  if (game.initialFen && game.initialFen !== INITIAL_FEN) tags.push(['FEN', game.initialFen]);
  if (tags.length) txt = tags.map(t => '[' + t[0] + ' "' + t[1] + '"]').join('\n') + '\n\n';
  return txt;
}

export const renderFullTxt = (ctrl: AnalyseCtrl): string =>
  renderPgnTags(ctrl.data.game) + renderNodesTxt(ctrl.tree.root, true);

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

export function renderVariationPgn(game: Game, nodeList: TreeNode[]): string {
  const filteredNodeList = nodeList.filter(node => node.san);
  if (filteredNodeList.length === 0) return '';

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

  return renderPgnTags(game) + variationPgn;
}
