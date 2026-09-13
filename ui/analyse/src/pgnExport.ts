import { INITIAL_FEN } from 'chessops/fen';
import { h } from 'snabbdom';

import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import { plyPrefix, renderNodesTxt } from 'lib/game/nodePGN';
import type { TreeNodeLite } from 'lib/tree/types';
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

export const renderFullTxt = (ctrl: AnalyseCtrl, root: TreeNodeLite = ctrl.tree.root): string =>
  renderPgnTags(ctrl.data.game) + renderNodesTxt(root, true);

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

export function renderNodesPgn(game: Game, nodeList: TreeNodeLite[], includeSubVariations: boolean): string {
  const nonRootNodes = nodeList.filter(node => node.san);
  let pgn = '';

  if (nonRootNodes.length) {
    const first = nonRootNodes[0];
    pgn += `${plyPrefix(first)}${first.san} `;

    for (let i = 1; i < nonRootNodes.length; i++) {
      const node = nonRootNodes[i];
      if (node.ply % 2 === 1) {
        pgn += plyToTurn(node.ply) + '. ';
      }

      pgn += fixCrazySan(node.san!) + ' ';
    }
  }

  pgn += renderNodesTxt(nodeList[nodeList.length - 1], nonRootNodes.length === 0, includeSubVariations);

  return pgn ? renderPgnTags(game) + pgn : '';
}
