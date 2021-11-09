import AnalyseCtrl from './ctrl';
import { h } from 'snabbdom';
import { MaybeVNodes } from './interfaces';
import { notationStyle } from 'common/notation';
import { ForecastStep } from './forecast/interfaces';
import { ops as treeOps } from 'tree';

import { makeKifHeader } from 'shogiops/kif';
import { makeCsaHeader } from 'shogiops/csa';
import { kifDestSquare, kifOrigSquare } from 'shogiops/kifUtil';
import { makeCsaSquare } from 'shogiops/csaUtil';
import { parseFen, INITIAL_FEN } from 'shogiops/fen';
import { defined } from 'common';
import { roleTo2Kanji, roleToCsa } from 'shogiops/util';
import { isDrop, Square, Role, Move } from 'shogiops/types';
import { lishogiCharToRole, parseLishogiUci } from 'shogiops/compat';
import { renderTime } from './clocks';
import { promote } from 'shogiops/variantUtil';

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
        lastDest = move.to;
      }
    }
    if (defined(m.comments)) {
      for (const c of m.comments) {
        res.push('* ' + c.text);
      }
    }
  }

  for (const m of mainline.reverse()) {
    for (const m2 of m.children.slice(1)) {
      res.push('\n変化：' + m2.ply + '手');
      res.push(...renderKifNodes(m2, offset));
    }
  }

  return res;
}

export function renderFullKif(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game;
  const setup = parseFen(g.initialFen ?? INITIAL_FEN).unwrap();
  const offset = ctrl.data.game.startedAtTurn % 2;

  const moves = renderKifNodes(ctrl.tree.root, offset % 2).join('\n');

  const tags = ctrl.data.tags ?? [];
  // We either don't want to display these or we display them through other means
  const unwatedTagNames = ['先手', '下手', '後手', '上手', '手合割', '図', 'FEN', 'Result'];
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
    moves,
  ].join('\n');
}

function renderCsaTime(moveTime: number): string {
  return ',T' + Math.floor(moveTime / 100);
}

function renderCsaMove(move: Move, role: Role, color: Color): string {
  if (isDrop(move)) {
    return (color === 'sente' ? '+' : '-') + '00' + makeCsaSquare(move.to) + roleToCsa(role);
  } else {
    return (
      (color === 'sente' ? '+' : '-') +
      makeCsaSquare(move.from) +
      makeCsaSquare(move.to) +
      roleToCsa(move.promotion ? promote('shogi')(role) : role)
    );
  }
}

function renderCsaMainline(node: Tree.Node): string[] {
  const res: string[] = [];

  const mainline = treeOps.mainlineNodeList(node);

  for (const m of mainline) {
    if (defined(m.san) && defined(m.uci)) {
      const move = parseLishogiUci(m.uci);
      const role = lishogiCharToRole(m.san[0]);
      if (defined(move) && defined(role)) {
        const csaMove = renderCsaMove(move, role, m.ply % 2 ? 'sente' : 'gote');
        const csaTime = defined(m.clock) ? renderCsaTime(m.clock) : '';
        res.push(csaMove + csaTime);
      }
    }
    if (defined(m.comments)) {
      for (const c of m.comments) {
        res.push("'" + c.text);
      }
    }
  }

  return res;
}

function processCsaTags(tags: string[][]): string[] {
  function kifTagToCsaTag(kifTag: string[]): string {
    switch (kifTag[0]) {
      case '開始日時':
        return `$START_TIME:${kifTag[1]}`;
      case '終了日時':
        return `$END_TIME:${kifTag[1]}`;
      case '棋戦':
        return `$EVENT:${kifTag[1]}`;
      case '場所':
        return `$SITE:${kifTag[1]}`;
      case '持ち時間':
        return `$TIME_LIMIT:${kifTag[1].replace(/[^\d+\|]/g, '')}`;
      case '戦型':
        return `$OPENING:${kifTag[1]}`;
      case '先手':
        return `N+${kifTag[1]}`;
      case '後手':
        return `N-${kifTag[1]}`;
      default:
        return '';
    }
  }
  // CSA shouldn't contain non-ascii characters (except for comments)
  // allow non-ascii characters in values, but not in keys
  const asciiTags = tags
    .filter(t => /^[\x20-\x7F]+$/.test(t[0]) && !['FEN', 'Result'].includes(t[0]))
    .map(t => `$${t[0]}:${t[1]}`);
  return tags
    .map(t => kifTagToCsaTag(t))
    .sort((a, b) => (a[0] === 'N' ? (b[0] === 'N' ? 0 : -1) : 1)) // so we have names on top
    .filter(t => t.length > 0)
    .concat(asciiTags);
}

export function renderFullCsa(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game;
  const tags = processCsaTags(ctrl.data.tags ?? []);
  const setup = parseFen(g.initialFen ?? INITIAL_FEN).unwrap();
  const moves = renderCsaMainline(ctrl.tree.root).join('\n');
  return [...tags, makeCsaHeader(setup), moves].join('\n');
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
