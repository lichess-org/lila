import { makeKifHeader, makeKifMove } from 'shogiops/notation/kif/kif';
import { makeCsaHeader, makeCsaMove } from 'shogiops/notation/csa/csa';
import { initialSfen, parseSfen } from 'shogiops/sfen';
import { Position } from 'shogiops/shogi';
import { Square } from 'shogiops/types';
import { parseUsi } from 'shogiops/util';
import { defined } from 'common/common';
import { ops as treeOps } from 'tree';

import AnalyseCtrl from './ctrl';
import { renderTime } from './clocks';

function makeKifTime(moveTime: number, totalTime: number): string {
  return '   (' + renderTime(moveTime, false) + '/' + renderTime(totalTime, true) + ')';
}

function pad(str: string, size: number): string {
  while (str.length < size) str = ' ' + str;
  return str + ' ';
}

function makeKifNodes(node: Tree.Node, pos: Position, offset: number): string[] {
  pos = pos.clone();
  const res: string[] = [];

  const mainline = treeOps.mainlineNodeList(node);
  const padding = mainline.length.toString().length;
  let lastDest: Square | undefined = undefined;
  let timesSoFar: number[] = [0, 0];

  for (const m of mainline) {
    if (defined(m.usi)) {
      const move = parseUsi(m.usi);
      if (defined(move)) {
        const kifMove = makeKifMove(pos, move, lastDest);
        const kifTime = defined(m.clock) ? makeKifTime(m.clock, (timesSoFar[m.ply % 2] += m.clock)) : '';
        res.push(pad((m.ply - offset).toString(), padding + 1) + kifMove + kifTime);
        lastDest = move.to;
        pos.play(move);
      }
    }
    if (defined(m.comments)) {
      for (const c of m.comments) {
        res.push('* ' + c.text);
      }
    }
  }

  for (const m of mainline.reverse()) {
    const newPos = parseSfen(pos.rules, m.sfen, false);
    if (newPos.isOk) {
      for (const m2 of m.children.slice(1)) {
        res.push('\n変化：' + m2.ply + '手');
        res.push(...makeKifNodes(m2, newPos.value, offset));
      }
    }
  }

  return res;
}

export function renderFullKif(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game;
  const offset = ctrl.plyOffset();

  const pos = parseSfen(
    ctrl.data.game.variant.key,
    g.initialSfen || initialSfen(ctrl.data.game.variant.key),
    false
  ).unwrap();
  const moves = makeKifNodes(ctrl.tree.root, pos, offset % 2).join('\n');

  const tags = ctrl.data.tags ?? [];
  // We either don't want to display these or we display them through other means
  const unwatedTagNames = ['先手', '下手', '後手', '上手', '手合割', '図', 'Sfen', 'Result', 'Variant'];
  const otherTags = tags.filter(t => !unwatedTagNames.includes(t[0])).map(t => t[0] + '：' + t[1]);

  // We want these even empty
  const sente = tags.find(t => t[0] === '先手' || t[0] === '下手') ?? ['先手', ''];
  const gote = tags.find(t => t[0] === '後手' || t[0] === '上手') ?? ['後手', ''];

  return [
    ...otherTags,
    makeKifHeader(pos),
    sente.join('：'),
    gote.join('：'),
    '手数----指手---------消費時間--',
    moves,
  ].join('\n');
}

function makeCsaTime(moveTime: number): string {
  return ',T' + Math.floor(moveTime / 100);
}

function makeCsaMainline(node: Tree.Node, pos: Position): string[] {
  pos = pos.clone();
  const res: string[] = [];

  const mainline = treeOps.mainlineNodeList(node);

  for (const m of mainline) {
    if (defined(m.usi)) {
      const move = parseUsi(m.usi);
      if (defined(move)) {
        const csaMove = makeCsaMove(pos, move);
        const csaTime = defined(m.clock) ? makeCsaTime(m.clock) : '';
        res.push(csaMove + csaTime);
        pos.play(move);
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
    .filter(t => /^[\x20-\x7F]+$/.test(t[0]) && !['SFEN', 'Result'].includes(t[0]))
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
  const pos = parseSfen('standard', g.initialSfen ?? initialSfen('standard'), false).unwrap();
  const moves = makeCsaMainline(ctrl.tree.root, pos).join('\n');
  return [...tags, makeCsaHeader(pos), moves].join('\n');
}
