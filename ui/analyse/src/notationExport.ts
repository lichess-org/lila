import { defined } from 'common/common';
import { makeCsaHeader, makeCsaMove } from 'shogiops/notation/csa/csa';
import { makeKifHeader, makeKifMove } from 'shogiops/notation/kif/kif';
import { initialSfen, parseSfen } from 'shogiops/sfen';
import { Square } from 'shogiops/types';
import { parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { Shogi } from 'shogiops/variant/shogi';
import { ops as treeOps } from 'tree';
import { renderTime } from './clocks';
import AnalyseCtrl from './ctrl';
import { isHandicap } from 'shogiops/handicaps';

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

  const moveNumberSuf = pos.rules === 'chushogi' ? '手目' : '';

  for (const m of mainline) {
    if (defined(m.usi)) {
      const move = parseUsi(m.usi);
      if (defined(move)) {
        const kifMove = makeKifMove(pos, move, lastDest),
          kifTime = defined(m.clock) ? makeKifTime(m.clock, (timesSoFar[m.ply % 2] += m.clock)) : '',
          moveNumStr = pad((m.ply - offset).toString(), padding + 1) + moveNumberSuf;
        if (kifMove?.includes('\n')) {
          const split = kifMove.split('\n');
          res.push(moveNumStr + split[0]);
          res.push(moveNumStr + split[1] + kifTime);
        } else res.push(`${moveNumStr} ${kifMove}${kifTime}`);
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
const toKanji = {
  Start: '開始日時',
  End: '終了日時',
  Event: '棋戦',
  Site: '場所',
  Opening: '戦型',
  Timecontrol: '持ち時間',
  Byoyomi: '秒読み',
  Handicap: '手合割',
  Sente: '先手',
  Gote: '後手',
  Senteteam: '先手のチーム',
  Goteteam: '後手のチーム',
  Annotator: '注釈者',
  Termination: '図',
  Problemname: '作品名',
  Problemid: '作品番号',
  Composer: '作者',
  Dateofpublicatio: '発表年月',
  Publication: '発表誌',
  Collection: '出典',
  Length: '手数',
  Prize: '受賞',
};
export function renderFullKif(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game,
    offset = ctrl.plyOffset(),
    sfen = g.initialSfen || initialSfen(ctrl.data.game.variant.key);

  const pos = parseSfen(ctrl.data.game.variant.key, sfen, false).unwrap(),
    moves = makeKifNodes(ctrl.tree.root, pos, offset % 2).join('\n');

  const tags = ctrl.data.tags ?? [],
    handicap = isHandicap({ sfen: sfen, rules: g.variant.key }),
    colorTags = [handicap ? '下手' : '先手', handicap ? '上手' : '後手'];
  // We either don't want to display these or we display them through other means
  const unwatedTagNames = ['Sente', 'Shitate', 'Gote', 'Uwate', 'Handicap', 'Termination', 'Sfen', 'Result', 'Variant'],
    otherTags = tags.filter(t => !unwatedTagNames.includes(t[0])).map(t => (toKanji[t[0]] || t[0]) + '：' + t[1]);

  // We want these even empty
  const sente = tags.find(t => t[0] === 'Sente' || t[0] === 'Shitate')?.[1] ?? '',
    gote = tags.find(t => t[0] === 'Gote' || t[0] === 'Uwate')?.[1] ?? '';

  return [
    ...otherTags,
    makeKifHeader(pos),
    `${colorTags[0]}：${sente}`,
    `${colorTags[1]}：${gote}`,
    '手数----指手---------消費時間--',
    moves,
  ]
    .filter(l => l.length)
    .join('\n');
}

function makeCsaTime(moveTime: number): string {
  return ',T' + Math.floor(moveTime / 100);
}

function makeCsaMainline(node: Tree.Node, pos: Shogi): string[] {
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
    switch (kifTag[0].toLowerCase()) {
      case 'start':
        return `$START_TIME:${kifTag[1]}`;
      case 'end':
        return `$END_TIME:${kifTag[1]}`;
      case 'event':
        return `$EVENT:${kifTag[1]}`;
      case 'site':
        return `$SITE:${kifTag[1]}`;
      case 'timecontrol':
        return `$TIME_LIMIT:${kifTag[1].replace(/[^\d+\|]/g, '')}`;
      case 'opening':
        return `$OPENING:${kifTag[1]}`;
      case 'sente':
      case 'shitate':
        return `N+${kifTag[1]}`;
      case 'gote':
      case 'uwate':
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
