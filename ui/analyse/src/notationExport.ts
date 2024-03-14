import { defined } from 'common/common';
import { makeCsaHeader, makeCsaMoveOrDrop } from 'shogiops/notation/csa/csa';
import { makeKifHeader, makeKifMoveOrDrop } from 'shogiops/notation/kif/kif';
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
        const kifMove = makeKifMoveOrDrop(pos, move, lastDest),
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
export function tagToKif(tag: string, handicap: boolean): string | undefined {
  switch (tag.toLowerCase()) {
    case 'start':
      return '開始日時';
    case 'end':
      return '終了日時';
    case 'event':
      return '棋戦';
    case 'site':
      return '場所';
    case 'opening':
      return '戦型';
    case 'timecontrol':
      return '持ち時間';
    case 'byoyomi':
      return '秒読み';
    case 'handicap':
      return '手合割';
    case 'sente':
      return handicap ? '下手' : '先手';
    case 'gote':
      return handicap ? '上手' : '後手';
    case 'senteteam':
      return handicap ? '下手のチーム' : '先手のチーム';
    case 'goteteam':
      return handicap ? '上手のチーム' : '後手のチーム';
    case 'senteelo':
      return handicap ? '下手のELO' : '先手のELO';
    case 'goteelo':
      return handicap ? '上手のELO' : '後手のELO';
    case 'annotator':
      return '注釈者';
    case 'termination':
      return '図';
    case 'problemname':
      return '作品名';
    case 'problemid':
      return '作品番号';
    case 'composer':
      return '作者';
    case 'dateofpublication':
      return '発表年月';
    case 'publication':
      return '発表誌';
    case 'collection':
      return '出典';
    case 'length':
      return '手数';
    case 'prize':
      return '受賞';
    default:
      return undefined;
  }
}
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
  const unwatedTagNames = ['Sente', 'Gote', 'Handicap', 'Termination', 'Result'],
    otherTags = tags
      .filter(t => !unwatedTagNames.includes(t[0]))
      .map(t => (tagToKif(t[0], handicap) || t[0]) + '：' + t[1]);

  // We want these even empty
  const sente = tags.find(t => t[0] === 'Sente')?.[1] ?? '',
    gote = tags.find(t => t[0] === 'Gote')?.[1] ?? '';

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
        const csaMove = makeCsaMoveOrDrop(pos, move);
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
  function tagToCsa(tagWithValue: string[]): string {
    switch (tagWithValue[0].toLowerCase()) {
      case 'start':
        return `$START_TIME:${tagWithValue[1]}`;
      case 'end':
        return `$END_TIME:${tagWithValue[1]}`;
      case 'event':
        return `$EVENT:${tagWithValue[1]}`;
      case 'site':
        return `$SITE:${tagWithValue[1]}`;
      case 'timecontrol':
        return `$TIME_LIMIT:${tagWithValue[1].replace(/[^\d+\|]/g, '')}`;
      case 'opening':
        return `$OPENING:${tagWithValue[1]}`;
      case 'sente':
        return `N+${tagWithValue[1]}`;
      case 'gote':
        return `N-${tagWithValue[1]}`;
      default:
        return '';
    }
  }
  // CSA shouldn't contain non-ascii characters (except for comments)
  // allow non-ascii characters in values, but not in keys
  function asciiTag(tag: string[]): string {
    if (/^[\x20-\x7F]+$/.test(tag[0]) && !['Result', 'Handicap'].includes(tag[0]))
      return `$${tag[0].toUpperCase()}:${tag[1]}`;
    else return '';
  }
  return tags
    .map(t => tagToCsa(t) || asciiTag(t))
    .sort((a, b) => (a[0] === 'N' ? (b[0] === 'N' ? 0 : -1) : 1)) // so we have names on top
    .filter(t => t.length > 0);
}

export function renderFullCsa(ctrl: AnalyseCtrl): string {
  const g = ctrl.data.game;
  const tags = processCsaTags(ctrl.data.tags ?? []);
  const pos = parseSfen('standard', g.initialSfen ?? initialSfen('standard'), false).unwrap();
  const moves = makeCsaMainline(ctrl.tree.root, pos).join('\n');
  return [...tags, makeCsaHeader(pos), moves].join('\n');
}
