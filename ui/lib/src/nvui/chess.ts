import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { type Pieces, files } from 'chessground/types';
import { type Setting, makeSetting } from './setting';
import { parseFen } from 'chessops/fen';
import { chessgroundDests, lichessRules } from 'chessops/compat';
import { COLORS, RANK_NAMES, ROLES, type FileName } from 'chessops/types';
import { setupPosition } from 'chessops/variant';
import { charToRole, opposite, parseUci, roleToChar } from 'chessops/util';
import { destsToUcis, plyToTurn, sanToUci, sanToWords, sanWriter } from '../game/chess';
import { storage } from '../storage';

const moveStyles = ['uci', 'san', 'literate', 'nato', 'anna'] as const;
export type MoveStyle = (typeof moveStyles)[number];
const pieceStyles = ['letter', 'white uppercase letter', 'name', 'white uppercase name'] as const;
type PieceStyle = (typeof pieceStyles)[number];
const prefixStyles = ['letter', 'name', 'none'] as const;
type PrefixStyle = (typeof prefixStyles)[number];
type PositionStyle = 'before' | 'after' | 'none';
type BoardStyle = 'plain' | 'table';

interface RoundStep {
  uci?: Uci;
  fen: FEN;
}

const nato: { [file in Files]: string } = {
  a: 'alpha',
  b: 'bravo',
  c: 'charlie',
  d: 'delta',
  e: 'echo',
  f: 'foxtrot',
  g: 'golf',
  h: 'hotel',
};
const anna: { [file in Files]: string } = {
  a: 'anna',
  b: 'bella',
  c: 'cesar',
  d: 'david',
  e: 'eva',
  f: 'felix',
  g: 'gustav',
  h: 'hector',
};

export function boardSetting(): Setting<BoardStyle> {
  return makeSetting<BoardStyle>({
    choices: [
      ['plain', 'plain: layout with no semantic rows or columns'],
      ['table', 'table: layout using a table with rank and file columns and row headers'],
    ],
    default: 'plain',
    storage: storage.make('nvui.boardLayout'),
  });
}

export function styleSetting(): Setting<MoveStyle> {
  return makeSetting<MoveStyle>({
    choices: moveStyles.map(s => [s, `${s}: ${renderSan('Nxf3', 'g1f3', s)}`]),
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: storage.make('nvui.moveNotation'),
  });
}

export function pieceSetting(): Setting<PieceStyle> {
  return makeSetting<PieceStyle>({
    choices: pieceStyles.map(p => [p, `${p}: ${renderPieceStyle('P', p)}`]),
    default: 'letter',
    storage: storage.make('nvui.pieceStyle'),
  });
}

export function prefixSetting(): Setting<PrefixStyle> {
  return makeSetting<PrefixStyle>({
    choices: prefixStyles.map(p => [p, `${p}: ${renderPrefixStyle('white', p)}`]),
    default: 'letter',
    storage: storage.make('nvui.prefixStyle'),
  });
}

export function positionSetting(): Setting<PositionStyle> {
  return makeSetting<PositionStyle>({
    choices: [
      ['before', 'before: c2: wp'],
      ['after', 'after: wp: c2'],
      ['none', 'none'],
    ],
    default: 'before',
    storage: storage.make('nvui.positionStyle'),
  });
}

const renderPieceStyle = (ch: string, pieceStyle: PieceStyle) =>
  pieceStyle === 'letter'
    ? ch.toLowerCase()
    : pieceStyle === 'white uppercase letter'
      ? ch
      : pieceStyle === 'name'
        ? charToRole(ch)!
        : `${ch.replace('N', 'K').replace('n', 'k')}${charToRole(ch)!.slice(1)}`;

const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle): `${Color} ` | 'w' | 'b' | '' =>
  prefixStyle === 'letter' ? (color[0] as 'w' | 'b') : prefixStyle === 'name' ? `${color} ` : '';

const renderPieceStr = (ch: string, pieceStyle: PieceStyle, c: Color, prefixStyle: PrefixStyle): string =>
  `${renderPrefixStyle(c, prefixStyle)} ${renderPieceStyle(c === 'white' ? ch.toUpperCase() : ch, pieceStyle)}`;

export const renderSan = (san: San | undefined, uci: Uci | undefined, style: MoveStyle): string =>
  !san
    ? 'Game start'
    : style === 'uci'
      ? (uci ?? '')
      : style === 'san'
        ? san
        : sanToWords(san)
            .split(' ')
            .map(f =>
              files.includes(f.toLowerCase() as FileName)
                ? renderFile(f.toLowerCase() as FileName, style)
                : f,
            )
            .join(' ');

export const renderPieces = (pieces: Pieces, style: MoveStyle): VNode =>
  h(
    'div.pieces',
    COLORS.map(color =>
      h(`div.${color}-pieces`, [
        h('h3', `${color} pieces`),
        ROLES.slice()
          .reverse()
          .reduce<{ role: Role; keys: Key[] }[]>(
            (lists, role) =>
              lists.concat({
                role,
                keys: keysWithPiece(pieces, role, color),
              }),
            [],
          )
          .filter(l => l.keys.length)
          .map(
            l =>
              `${l.role}${l.keys.length > 1 ? 's' : ''}: ${l.keys.map(k => renderKey(k, style)).join(', ')}`,
          )
          .join(', '),
      ]),
    ),
  );

export const renderPockets = (pockets: Tree.NodeCrazy['pockets']): VNode[] =>
  COLORS.map((color, i) => h('h2', `${color} pocket: ${pocketsStr(pockets[i])}`));

export const pocketsStr = (pocket: Tree.CrazyPocket): string =>
  Object.entries(pocket)
    .map(([role, count]) => `${role}: ${count}`)
    .join(', ');

const keysWithPiece = (pieces: Pieces, role?: Role, color?: Color): Key[] =>
  Array.from(pieces).reduce<Key[]>(
    (keys, [key, p]) => (p.color === color && p.role === role ? keys.concat(key) : keys),
    [],
  );

export function renderPieceKeys(pieces: Pieces, p: string, style: MoveStyle): string {
  const color: Color = p === p.toUpperCase() ? 'white' : 'black';
  const role = charToRole(p)!;
  const keys = keysWithPiece(pieces, role, color);
  return `${color} ${role}: ${keys.length ? keys.map(k => renderKey(k, style)).join(', ') : i18n.site.none}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: MoveStyle): string {
  const renderedKeysWithPiece = Array.from(pieces)
    .sort(([key1], [key2]) => key1.localeCompare(key2))
    .reduce<string[]>(
      (acc, [key, p]) =>
        key.includes(rankOrFile) ? acc.concat(`${renderKey(key, style)} ${p.color} ${p.role}`) : acc,
      [],
    );
  return renderedKeysWithPiece.length ? renderedKeysWithPiece.join(', ') : i18n.site.none;
}

export function renderBoard(
  pieces: Pieces,
  pov: Color,
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
  positionStyle: PositionStyle,
  boardStyle: BoardStyle,
): VNode {
  const doRankHeader = (rank: Ranks): VNode => h('th', { attrs: { scope: 'row' } }, rank);

  const doFileHeaders = (): VNode => {
    const ths = files.map(file => h('th', { attrs: { scope: 'col' } }, file));
    return h('tr', [h('td'), ...(pov === 'black' ? ths.reverse() : ths), h('td')]);
  };

  const renderPositionStyle = (rank: Ranks, file: Files, orig: string) =>
    positionStyle === 'before'
      ? file.toUpperCase() + rank + ' ' + orig
      : positionStyle === 'after'
        ? orig + ' ' + file.toUpperCase() + rank
        : orig;

  const doPieceButton = (
    rank: Ranks,
    file: Files,
    letter: string,
    color: Color | 'none',
    text: string,
  ): VNode =>
    h(
      'button',
      {
        attrs: { rank: rank, file: file, piece: letter.toLowerCase(), color: color, 'trap-bypass': true },
      },
      renderPositionStyle(rank, file, text),
    );

  const doPiece = (rank: Ranks, file: Files): VNode => {
    const key: Key = `${file}${rank}`;
    const piece = pieces.get(key);
    const pieceWrapper = boardStyle === 'table' ? 'td' : 'span';
    if (piece) {
      const roleCh = roleToChar(piece.role);
      const pieceText = renderPieceStr(roleCh, pieceStyle, piece.color, prefixStyle);
      return h(pieceWrapper, doPieceButton(rank, file, roleCh, piece.color, pieceText));
    } else {
      const plusOrMinus = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      return h(pieceWrapper, doPieceButton(rank, file, plusOrMinus, 'none', plusOrMinus));
    }
  };

  const doRank = (pov: Color, rank: Ranks): VNode => {
    const rankElements: VNode[] = [];
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    rankElements.push(...files.map(file => doPiece(rank, file)));
    if (boardStyle === 'table') rankElements.push(doRankHeader(rank));
    if (pov === 'black') rankElements.reverse();
    return h(boardStyle === 'table' ? 'tr' : 'div', rankElements);
  };

  const ranks: VNode[] = [];
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  ranks.push(
    ...RANK_NAMES.slice()
      .reverse()
      .map(rank => doRank(pov, rank)),
  );
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  if (pov === 'black') ranks.reverse();
  return h(boardStyle === 'table' ? 'table.board-wrapper' : 'div.board-wrapper', ranks);
}

export const renderFile = (f: Files, style: MoveStyle): string =>
  style === 'nato' ? nato[f] : style === 'anna' ? anna[f] : f;

export const renderKey = (key: Key, style: MoveStyle): string =>
  style === 'nato' || style === 'anna' ? `${renderFile(key[0] as Files, style)} ${key[1]}` : key;

export function castlingFlavours(input: string): string {
  switch (input.toLowerCase().replace(/[-\s]+/g, '')) {
    case 'oo':
    case '00':
      return 'o-o';
    case 'ooo':
    case '000':
      return 'o-o-o';
  }
  return input;
}

/* Listen to interactions on the chessboard */
export function positionJumpHandler() {
  return (ev: KeyboardEvent): void => {
    const key = keyFromAttrs(ev.target as HTMLElement);
    const digitMatch = ev.code.match(/^Digit([1-8])$/);
    if (!digitMatch || !key) return;
    const newRank = ev.shiftKey ? key[1] : digitMatch[1];
    const newFile = ev.shiftKey ? files[Number(digitMatch[1]) - 1] : key[0];
    document.querySelector<HTMLElement>(squareSelector(newRank, newFile))?.focus();
  };
}

export function pieceJumpingHandler(selectSound: () => void, errorSound: () => void) {
  return (ev: KeyboardEvent): void => {
    const $currBtn = $(ev.target as HTMLElement);

    // TODO: decouple from promotion attribute setting in selectionHandler
    if ($currBtn.attr('promotion') === 'true') {
      const $moveBox = $('input.move');
      const $boardLive = $('.boardstatus');
      const promotionPiece = ev.key.toLowerCase();
      if (!promotionPiece.match(/^[qnrb]$/)) {
        $boardLive.text('Invalid promotion piece. q for queen, n for knight, r for rook, b for bishop');
        return;
      }
      $moveBox.val($moveBox.val() + promotionPiece);
      $currBtn.removeAttr('promotion');
      $('#move-form').trigger('submit');
    }

    const myBtnAttrs = squareSelector($currBtn.attr('rank') ?? '', $currBtn.attr('file') ?? '');
    const $allPieces = $(`.board-wrapper [piece="${ev.key.toLowerCase()}"], ${myBtnAttrs}`);
    const myPieceIndex = $allPieces.index(myBtnAttrs);
    const next = ev.key.toLowerCase() === ev.key;
    const $prevNextPieces = next ? $allPieces.slice(myPieceIndex + 1) : $allPieces.slice(0, myPieceIndex);
    const pieceEl = next ? $prevNextPieces.get(0) : $prevNextPieces.get($prevNextPieces.length - 1);
    if (pieceEl) pieceEl.focus();
    // if detected any matching piece; one is the piece being clicked on,
    else if ($allPieces.length >= 2) {
      const wrapPieceEl = next ? $allPieces.get(0) : $allPieces.get($allPieces.length - 1);
      wrapPieceEl?.focus();
      selectSound();
    } else errorSound();
  };
}

export function arrowKeyHandler(pov: Color, borderSound: () => void) {
  return (ev: KeyboardEvent): void => {
    const isWhite = pov === 'white';
    const key = keyFromAttrs(ev.target as HTMLElement);
    if (!key) return;
    let file = key[0];
    let rank = Number(key[1]);
    if (ev.key === 'ArrowUp') rank = isWhite ? (rank += 1) : (rank -= 1);
    else if (ev.key === 'ArrowDown') rank = isWhite ? (rank -= 1) : (rank += 1);
    else if (ev.key === 'ArrowLeft')
      file = String.fromCharCode(isWhite ? file.charCodeAt(0) - 1 : file.charCodeAt(0) + 1);
    else if (ev.key === 'ArrowRight')
      file = String.fromCharCode(isWhite ? file.charCodeAt(0) + 1 : file.charCodeAt(0) - 1);
    const newSqEl = document.querySelector<HTMLElement>(squareSelector(`${rank}`, file));
    newSqEl ? newSqEl.focus() : borderSound();
    ev.preventDefault();
  };
}

export function selectionHandler(getOpponentColor: () => Color, selectSound: () => void) {
  return (ev: MouseEvent): void => {
    const opponentColor = getOpponentColor();
    // this depends on the current document structure. This may not be advisable in case the structure wil change.
    const $evBtn = $(ev.target as HTMLElement);
    const rank = $evBtn.attr('rank');
    const pos = ($evBtn.attr('file') ?? '') + rank;
    const $boardLive = $('.boardstatus');
    const promotionRank = opponentColor === 'black' ? '8' : '1';
    const $moveBox = $('input.move');
    if (!$moveBox.length) return;

    // user can select their own piece again if they change their mind
    if ($moveBox.val() !== '' && $evBtn.attr('color') === opposite(opponentColor)) {
      $moveBox.val('');
    }

    // if no move in box yet
    if ($moveBox.val() === '') {
      // if user selects another's piece first
      if ($evBtn.attr('color') === opponentColor) return;
      // as long as the user is selecting a piece and not a blank tile
      if ($evBtn.text().match(/^[^\-+]+/g)) {
        $moveBox.val(pos);
        selectSound();
      }
    } else {
      const first = $moveBox.val();
      if (typeof first !== 'string' || !isKey(first)) return;
      const $firstPiece = $(squareSelector(first[1], first[0]));
      $moveBox.val($moveBox.val() + pos);
      // this is coupled to pieceJumpingHandler() noticing that the attribute is set and acting differently. TODO: make cleaner
      // if pawn promotion
      if (rank === promotionRank && $firstPiece.attr('piece')?.toLowerCase() === 'p') {
        $evBtn.attr('promotion', 'true');
        $boardLive.text('Promote to? q for queen, n for knight, r for rook, b for bishop');
        return;
      }
      $('#move-form').trigger('submit');
    }
  };
}

export function boardCommandsHandler() {
  return (ev: KeyboardEvent): void => {
    const key = keyFromAttrs(ev.target as HTMLElement);
    const $boardLive = $('.boardstatus');
    if (ev.key === 'o' && key) $boardLive.text(key);
    else if (ev.key === 'l') $boardLive.text($('p.lastMove').text());
    else if (ev.key === 't') $boardLive.text(`${$('.nvui .botc').text()} - ${$('.nvui .topc').text()}`);
  };
}

export function lastCapturedCommandHandler(
  fensteps: () => string[],
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
) {
  const lastCaptured = (): string => {
    const fens = fensteps();
    const oldFen = fens[fens.length - 2];
    const currentFen = fens[fens.length - 1];
    if (!oldFen || !currentFen) return 'none';
    const oldBoardFen = oldFen.split(' ')[0];
    const currentBoardFen = currentFen.split(' ')[0];
    for (const p of 'kKqQrRbBnNpP') {
      const diff = oldBoardFen.split(p).length - 1 - (currentBoardFen.split(p).length - 1);
      const pcolor = p.toUpperCase() === p ? 'white' : 'black';
      if (diff === 1) return renderPieceStr(p, pieceStyle, pcolor, prefixStyle);
    }
    return 'none';
  };
  return (): Cash => $('.boardstatus').text(lastCaptured());
}

export function possibleMovesHandler(yourColor: Color, cg: CgApi, variant: VariantKey, steps: RoundStep[]) {
  return (ev: KeyboardEvent): void => {
    if (ev.key.toLowerCase() !== 'm') return;
    const pos = keyFromAttrs(ev.target as HTMLElement);
    if (!pos) return;
    const $boardLive = $('.boardstatus');

    // possible inefficient to reparse fen; but seems to work when it is AND when it is not the users' turn. Also note that this FEN is incomplete as it only contains the piece information.
    // if it is your turn
    const playThroughToFinalDests = (): Dests => {
      {
        const fromSetup = setupPosition(lichessRules(variant), parseFen(steps[0].fen).unwrap()).unwrap();
        steps.forEach(s => {
          if (s.uci) {
            const move = parseUci(s.uci);
            if (move) fromSetup.play(move);
          }
        });
        // important to override whose turn it is so only the users' own turns will show up
        fromSetup.turn = yourColor;
        return chessgroundDests(fromSetup);
      }
    };
    const rawMoves = cg.state.turnColor === yourColor ? cg.state.movable.dests : playThroughToFinalDests();
    const possibleMoves = rawMoves
      ?.get(pos)
      ?.map(i => {
        const p = cg.state.pieces.get(i);
        // logic to prevent 'capture rook' on own piece in chess960
        return p && p.color !== yourColor ? `${i} captures ${p.role}` : i;
      })
      ?.filter(i => ev.key === 'm' || i.includes('captures'));
    $boardLive.text(
      !possibleMoves ? 'None' : !possibleMoves.length ? 'No captures' : possibleMoves.join(', '),
    );
  };
}

const promotionRegex = /^([a-h]x?)?[a-h](1|8)=[kqnbr]$/;
const uciPromotionRegex = /^([a-h][1-8])([a-h](1|8))[kqnbr]$/;
const dropRegex = /^(([qrnb])@([a-h][1-8])|p?@([a-h][2-7]))$/;
export type DropMove = { role: Role; key: Key };

export function inputToMove(input: string, fen: string, chessground: CgApi): Uci | DropMove | undefined {
  const dests = chessground.state.movable.dests;
  if (!dests) return;
  const legalUcis = destsToUcis(dests),
    legalSans = sanWriter(fen, legalUcis),
    cleaned = input.replace(/\+|#/g, '');
  let uci = sanToUci(cleaned, legalSans) || cleaned,
    promotion = '';

  const drop = cleaned.match(dropRegex);
  if (drop) return { role: charToRole(cleaned[0]) || 'pawn', key: cleaned.split('@')[1].slice(0, 2) as Key };
  if (cleaned.match(promotionRegex)) {
    uci = sanToUci(cleaned.slice(0, -2), legalSans) || cleaned;
    promotion = cleaned.slice(-1).toLowerCase();
  } else if (cleaned.match(uciPromotionRegex)) {
    uci = cleaned.slice(0, -1);
    promotion = cleaned.slice(-1).toLowerCase();
  } else if ('18'.includes(uci[3]) && chessground.state.pieces.get(uci.slice(0, 2) as Key)?.role === 'pawn')
    promotion = 'q';

  return legalUcis.includes(uci.toLowerCase()) ? `${uci}${promotion}` : undefined;
}

export function renderMainline(nodes: Tree.Node[], currentPath: Tree.Path, style: MoveStyle): VNodeChildren {
  const res: VNodeChildren = [];
  let path: Tree.Path = '';
  nodes.forEach(node => {
    if (!node.san || !node.uci) return;
    path += node.id;
    const content: VNodeChildren = [
      node.ply & 1 ? plyToTurn(node.ply) + '. ' : null,
      renderSan(node.san, node.uci, style),
    ];
    res.push(h('move', { attrs: { p: path }, class: { active: path === currentPath } }, content));
    res.push(renderComments(node, style));
    res.push(', ');
    if (node.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

export const renderComments = (node: Tree.Node, style: MoveStyle): string =>
  node.comments?.map(c => ` ${augmentLichessComment(c, style)}`).join('.') ?? '';

const augmentLichessComment = (comment: Tree.Comment, style: MoveStyle): string =>
  comment.by === 'lichess'
    ? comment.text.replace(
        /([^\s]+) was best\./,
        (_, san) => `Best move was ${renderSan(san, undefined, style)}`,
      )
    : comment.text;

const squareSelector = (rank: string, file: string) =>
  `.board-wrapper button[rank="${rank}"][file="${file}"]`;

const isKey = (maybeKey: string): maybeKey is Key => !!maybeKey.match(/^[a-h][1-8]$/);

const keyFromAttrs = (el: HTMLElement): Key | undefined => {
  const maybeKey = `${el.getAttribute('file') ?? ''}${el.getAttribute('rank') ?? ''}`;
  return isKey(maybeKey) ? maybeKey : undefined;
};
