import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom';
import { MaybeVNodes } from './interfaces';
import { notationStyle } from 'common/notation';
import { ForecastStep } from './forecast/interfaces';
import { ops as treeOps } from 'tree';

import { makeKifHeader } from 'shogiops/kif';
import { kifDestSquare, kifOrigSquare } from 'shogiops/kifUtil';
import { parseFen, INITIAL_FEN } from 'shogiops/fen';
import { defined } from 'common';
import { roleTo2Kanji } from 'shogiops/util';
import { isDrop, Square, Role, Move } from 'shogiops/types';
import { lishogiCharToRole, parseLishogiUci } from 'shogiops/compat';
import { renderTime } from './clocks';

function renderKifMove(move: Move, role: Role, same = false): string {
  if (isDrop(move)) {
    return kifDestSquare(move.to) + roleTo2Kanji(role) + '打';
  } else {
    return (
      (same ? '同　' : kifDestSquare(move.to)) +
      roleTo2Kanji(role) +
      (move.promotion ? '成' : '') +
      '(' +
      kifOrigSquare(move.from) +
      ')'
    );
  }
}

function renderKifTime(moveTime: number, totalTime: number): string {
  return '   (' + renderTime(moveTime, false) + '/' + renderTime(totalTime, true) + ')';
}

function pad(str: string, size: number): string {
  while (str.length < size) str = ' ' + str;
  return str + ' ';
}

function renderKifNodes(node: Tree.Node, offset: number): string[] {
  const res: string[] = [];

  const mainline = treeOps.mainlineNodeList(node);
  const padding = mainline.length.toString().length;
  let lastDest: Square | undefined = undefined;
  let timesSoFar: number[] = [0, 0];

  for (const m of mainline) {
    if (defined(m.san) && defined(m.uci)) {
      const move = parseLishogiUci(m.uci);
      const role = lishogiCharToRole(m.san[0]);
      if (defined(move) && defined(role)) {
        const kifMove = renderKifMove(move, role, lastDest === move.to);
        const kifTime = defined(m.clock) ? renderKifTime(m.clock, (timesSoFar[m.ply % 2] += m.clock)) : '';
        res.push(pad((m.ply - offset).toString(), padding + 1) + kifMove + kifTime);
        if (defined(m.comments)) {
          for (const c of m.comments) {
            res.push('* ' + c.text);
          }
        }
        lastDest = move.to;
      }
    }
  }

  for (const m of mainline.reverse()) {
      for(const m2 of m.children.slice(1)) {
        res.push('\n変化：' + m2.ply + '手');
        res.push(...renderKifNodes(m2, offset));
      }
  }

  return res;
}

export function renderFullTxt(ctrl: AnalyseCtrl): string {
  console.log(ctrl);
  const g = ctrl.data.game;
  const setup = parseFen(g.initialFen ?? INITIAL_FEN).unwrap();
  const offset = ctrl.data.game.startedAtTurn % 2;

  const txt = renderKifNodes(ctrl.tree.root, offset % 2).join('\n');

  const tags = ctrl.data.tags ?? [];
  // We either don't want to display these or we display them through other means
  const unwatedTagNames = ['先手', '下手', '後手', '上手', '手合割'];
  const otherTags = tags.filter(t => !unwatedTagNames.includes(t[0])).map(t => t[0] + '：' + t[1]);

  // We want these even empty
  const sente = tags.find(t => t[0] === '先手' || t[0] === '下手') ?? ['先手', ''];
  const gote = tags.find(t => t[0] === '後手' || t[0] === '上手') ?? ['後手', ''];

  return [
    ...otherTags,
    makeKifHeader(setup),
    sente.join('：'),
    gote.join('：'),
    '手数----指手---------消費時間--',
    txt,
  ].join('\n');
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
