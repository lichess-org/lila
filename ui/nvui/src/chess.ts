import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { type Pieces, files } from 'chessground/types';
import { type Setting, makeSetting } from './setting';
import { parseFen } from 'chessops/fen';
import { chessgroundDests, lichessRules } from 'chessops/compat';
import { COLORS, RANK_NAMES, ROLES, type SquareName } from 'chessops/types';
import { setupPosition } from 'chessops/variant';
import { charToRole, opposite, parseUci, roleToChar } from 'chessops/util';
import { destsToUcis, plyToTurn, sanToUci, sanWriter } from 'chess';
import { storage } from 'common/storage';

const moveStyles = ['uci', 'san', 'literate', 'nato', 'anna'] as const;
export type MoveStyle = (typeof moveStyles)[number];
const pieceStyles = ['letter', 'white uppercase letter', 'name', 'white uppercase name'] as const;
export type PieceStyle = (typeof pieceStyles)[number];
const prefixStyles = ['letter', 'name', 'none'] as const;
export type PrefixStyle = (typeof prefixStyles)[number];
export type PositionStyle = 'before' | 'after' | 'none';
export type BoardStyle = 'plain' | 'table';

interface RoundStep {
  uci: Uci;
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
const skipToFile: { [letter: string]: Files } = {
  '!': 'a',
  '@': 'b',
  '#': 'c',
  $: 'd',
  '%': 'e',
  '^': 'f',
  '&': 'g',
  '*': 'h',
};

const symbolToFile = (char: string): Files => skipToFile[char] ?? '';

export const supportedVariant = (key: VariantKey): boolean =>
  !['antichess', 'racingKings', 'crazyhouse'].includes(key);

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
    choices: moveStyles.map(s => [s, `${s}: ${renderSan('Nxf3', undefined, s)}`]),
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

const renderPieceStyle = (piece: string, pieceStyle: PieceStyle) =>
  pieceStyle === 'letter'
    ? piece.toLowerCase()
    : pieceStyle === 'white uppercase letter'
      ? piece
      : pieceStyle === 'name'
        ? charToRole(piece)!
        : `${piece.replace('N', 'K').replace('n', 'k')}${charToRole(piece)!.slice(1)}`;

const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle): `${Color} ` | 'w' | 'b' | '' =>
  prefixStyle === 'letter' ? (color[0] as 'w' | 'b') : prefixStyle === 'name' ? `${color} ` : '';

export function lastCaptured(
  fensteps: () => string[],
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
): string {
  const fens = fensteps();
  const oldFen = fens[fens.length - 2];
  const newFen = fens[fens.length - 1];
  if (!oldFen || !newFen) return 'none';
  const oldSplitFen = oldFen.split(' ')[0];
  const newSplitFen = newFen.split(' ')[0];
  for (const p of 'kKqQrRbBnNpP') {
    const diff = oldSplitFen.split(p).length - 1 - (newSplitFen.split(p).length - 1);
    const pcolor = p.toUpperCase() === p ? 'white' : 'black';
    if (diff === 1) {
      const prefix = renderPrefixStyle(pcolor, prefixStyle);
      const piece = renderPieceStyle(p, pieceStyle);
      return prefix + piece;
    }
  }
  return 'none';
}

export function renderSan(san: San, uci: Uci | undefined, style: MoveStyle): string {
  if (!san) return '';
  let move: string;
  if (san.includes('O-O-O')) move = 'long castling';
  else if (san.includes('O-O')) move = 'short castling';
  else if (style === 'san') move = san.replace(/[\+#]/, '');
  else if (style === 'uci') move = uci || san;
  else {
    move = san
      .replace(/[\+#]/, '')
      .split('')
      .map(c => {
        if (c === 'x') return 'takes';
        if (c === '+') return 'check';
        if (c === '#') return 'checkmate';
        if (c === '=') return 'promotion';
        const code = c.charCodeAt(0);
        if (code > 48 && code < 58) return c; // 1-8
        if (code > 96 && code < 105) return renderFile(c as Files, style); // a-h
        return charToRole(c) || c;
      })
      .join(' ');
  }
  if (san.includes('+')) move += ' check';
  if (san.includes('#')) move += ' checkmate';
  return move;
}

export const renderPieces = (pieces: Pieces, style: MoveStyle): VNode =>
  h(
    'div',
    COLORS.map(color =>
      h('div', [
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

const keysWithPiece = (pieces: Pieces, role?: Role, color?: Color): Key[] =>
  Array.from(pieces).reduce<Key[]>(
    (keys, [key, p]) => (p.color === color && p.role === role ? keys.concat(key) : keys),
    [],
  );

export function renderPieceKeys(pieces: Pieces, p: string, style: MoveStyle): string {
  const color: Color = p === p.toUpperCase() ? 'white' : 'black';
  const role = charToRole(p)!;
  const keys = keysWithPiece(pieces, role, color);
  return `${color} ${role}: ${keys.length ? keys.map(k => renderKey(k, style)).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: MoveStyle): string {
  const renderedKeysWithPiece = Array.from(pieces).reduce<string[]>(
    (acc, [key, p]) =>
      key.includes(rankOrFile) ? acc.concat(`${renderKey(key, style)} ${p.color} ${p.role}`) : acc,
    [],
  );
  return renderedKeysWithPiece.length ? renderedKeysWithPiece.join(', ') : 'blank';
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
      text,
    );

  const doPiece = (rank: Ranks, file: Files): VNode => {
    const key: Key = `${file}${rank}`;
    const piece = pieces.get(key);
    const pieceWrapper = boardStyle === 'table' ? 'td' : 'span';
    if (piece) {
      const roleCh = roleToChar(piece.role);
      const pieceText = renderPieceStyle(piece.color === 'white' ? roleCh.toUpperCase() : roleCh, pieceStyle);
      const prefix = renderPrefixStyle(piece.color, prefixStyle);
      const text = renderPositionStyle(rank, file, prefix + pieceText);
      return h(pieceWrapper, doPieceButton(rank, file, roleCh, piece.color, text));
    } else {
      const letter = (key.charCodeAt(0) + key.charCodeAt(1)) % 2 ? '-' : '+';
      const text = renderPositionStyle(rank, file, letter);
      return h(pieceWrapper, doPieceButton(rank, file, letter, 'none', text));
    }
  };

  const doRank = (pov: Color, rank: Ranks): VNode => {
    const rankElements = [];
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
    const $btn = $(ev.target as HTMLElement);
    const file = $btn.attr('file') ?? '';
    const rank = $btn.attr('rank') ?? '';
    let newRank: string;
    let newFile: string;
    if (ev.key.match(/^[1-8]$/)) {
      newRank = ev.key;
      newFile = file;
    } else if (ev.key.match(/^[!@#$%^&*]$/)) {
      newRank = rank;
      newFile = symbolToFile(ev.key);
      // if not a valid key for jumping
    } else return;
    const newBtn = document.querySelector<HTMLElement>(
      '.board-wrapper button[rank="' + newRank + '"][file="' + newFile + '"]',
    );
    if (newBtn) newBtn.focus();
  };
}

export function pieceJumpingHandler(selectSound: () => void, errorSound: () => void) {
  return (ev: KeyboardEvent): void => {
    if (!ev.key.match(/^[kqrbnp]$/i)) return;
    const $currBtn = $(ev.target as HTMLElement);

    // TODO: decouple from promotion attribute setting in selectionHandler
    if ($currBtn.attr('promotion') === 'true') {
      const $moveBox = $('input.move');
      const $boardLive = $('.boardstatus');
      const promotionPiece = ev.key.toLowerCase();
      const $form = $moveBox.parent().parent();
      if (!promotionPiece.match(/^[qnrb]$/)) {
        $boardLive.text('Invalid promotion piece. q for queen, n for knight, r for rook, b for bishop');
        return;
      }
      $moveBox.val($moveBox.val() + promotionPiece);
      $currBtn.removeAttr('promotion');
      const sendForm = new Event('submit', {
        cancelable: true,
        bubbles: true,
      });
      $form.trigger(sendForm);
      return;
    }

    const myBtnAttrs = `.board-wrapper [rank="${$currBtn.attr('rank')}"][file="${$currBtn.attr('file')}"]`;
    const $allPieces = $('.board-wrapper [piece="' + ev.key.toLowerCase() + '"], ' + myBtnAttrs);
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
    const $currBtn = $(ev.target as HTMLElement);
    const isWhite = pov === 'white';
    let file = $currBtn.attr('file') ?? ' ';
    let rank = Number($currBtn.attr('rank'));
    if (ev.key === 'ArrowUp') rank = isWhite ? (rank += 1) : (rank -= 1);
    else if (ev.key === 'ArrowDown') rank = isWhite ? (rank -= 1) : (rank += 1);
    else if (ev.key === 'ArrowLeft')
      file = String.fromCharCode(isWhite ? file.charCodeAt(0) - 1 : file.charCodeAt(0) + 1);
    else if (ev.key === 'ArrowRight')
      file = String.fromCharCode(isWhite ? file.charCodeAt(0) + 1 : file.charCodeAt(0) - 1);
    else return;
    const newSqEl = document.querySelector<HTMLElement>(`.board-wrapper [file="${file}"][rank="${rank}"]`);
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
      // if user selects their own piece second
      if ($evBtn.attr('color') === opposite(opponentColor)) return;

      const first = $moveBox.val();
      const $firstPiece = $(`.board-wrapper [file="${first[0]}"][rank="${first[1]}"]`);
      $moveBox.val($moveBox.val() + pos);
      // this is coupled to pieceJumpingHandler() noticing that the attribute is set and acting differently. TODO: make cleaner
      // if pawn promotion
      if (rank === promotionRank && $firstPiece.attr('piece')?.toLowerCase() === 'p') {
        $evBtn.attr('promotion', 'true');
        $boardLive.text('Promote to? q for queen, n for knight, r for rook, b for bishop');
        return;
      }
      // this section depends on the form being the grandparent of the input.move box.
      const $form = $moveBox.parent().parent();
      const event = new Event('submit', {
        cancelable: true,
        bubbles: true,
      });
      $form.trigger(event);
    }
  };
}

export function boardCommandsHandler() {
  return (ev: KeyboardEvent): void => {
    const $currBtn = $(ev.target as HTMLElement);
    const $boardLive = $('.boardstatus');
    const position = ($currBtn.attr('file') ?? '') + ($currBtn.attr('rank') ?? '');
    if (ev.key === 'o') $boardLive.text(position);
    else if (ev.key === 'l') $boardLive.text($('p.lastMove').text());
    else if (ev.key === 't') $boardLive.text(`${$('.nvui .botc').text()}, ${$('.nvui .topc').text()}`);
  };
}

export function lastCapturedCommandHandler(
  fensteps: () => string[],
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
) {
  return (ev: KeyboardEvent): void => {
    if (ev.key === 'c') $('.boardstatus').text(lastCaptured(fensteps, pieceStyle, prefixStyle));
  };
}

export function possibleMovesHandler(
  yourColor: Color,
  turnColor: () => Color,
  startingFen: () => string,
  piecesFunc: () => Pieces,
  variant: VariantKey,
  moveable: () => Dests | undefined,
  steps: () => RoundStep[],
) {
  return (ev: KeyboardEvent): void => {
    if (ev.key.toLowerCase() !== 'm') return;
    const $boardLive = $('.boardstatus');
    const pieces: Pieces = piecesFunc();

    const $btn = $(ev.target as HTMLElement);
    const pos = (($btn.attr('file') ?? '') + $btn.attr('rank')) as SquareName;

    let rawMoves: Dests | undefined;

    // possible inefficient to reparse fen; but seems to work when it is AND when it is not the users' turn. Also note that this FEN is incomplete as it only contains the piece information.
    // if it is your turn
    if (turnColor() === yourColor) {
      rawMoves = moveable();
    } else {
      const fromSetup = setupPosition(lichessRules(variant), parseFen(startingFen()).unwrap()).unwrap();
      steps().forEach(s => {
        if (s.uci) {
          const move = parseUci(s.uci);
          if (move) fromSetup.play(move);
        }
      });
      // important to override whose turn it is so only the users' own turns will show up
      fromSetup.turn = yourColor;
      rawMoves = chessgroundDests(fromSetup);
    }

    const possibleCaptures = rawMoves
      ?.get(pos)
      ?.map(i => {
        const p = pieces.get(i);
        // logic to prevent 'capture rook' on own piece in chess960
        return p && p.color !== yourColor ? `${i} captures ${p.role}` : i;
      })
      ?.filter(i => ev.key === 'm' || i.includes('captures'));
    $boardLive.text(
      !possibleCaptures ? 'None' : !possibleCaptures.length ? 'No captures' : possibleCaptures.join(', '),
    );
  };
}

const promotionRegex = /^([a-h]x?)?[a-h](1|8)=\w$/;
const uciPromotionRegex = /^([a-h][1-8])([a-h](1|8))[qrbn]$/;

export function inputToLegalUci(input: string, fen: string, chessground: CgApi): string | undefined {
  const legalUcis = destsToUcis(chessground.state.movable.dests!),
    legalSans = sanWriter(fen, legalUcis);
  let uci = sanToUci(input, legalSans) || input,
    promotion = '';

  if (input.match(promotionRegex)) {
    uci = sanToUci(input.slice(0, -2), legalSans) || input;
    promotion = input.slice(-1).toLowerCase();
  } else if (input.match(uciPromotionRegex)) {
    uci = input.slice(0, -1);
    promotion = input.slice(-1).toLowerCase();
  } else if ('18'.includes(uci[3]) && chessground.state.pieces.get(uci.slice(0, 2) as Key)?.role === 'pawn')
    promotion = 'q';

  if (legalUcis.includes(uci.toLowerCase())) return uci + promotion;
  else return;
}

export function renderMainline(nodes: Tree.Node[], currentPath: Tree.Path, style: MoveStyle): VNodeChildren {
  const res: VNodeChildren = [];
  let path: Tree.Path = '';
  nodes.forEach(node => {
    if (!node.san || !node.uci) return;
    path += node.id;
    const content: VNodeChildren = [
      node.ply & 1 ? plyToTurn(node.ply) + ' ' : null,
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
  node.comments?.map(c => augmentLichessComment(c, style)).join('. ') ?? '';

const augmentLichessComment = (comment: Tree.Comment, style: MoveStyle): string =>
  comment.by === 'lichess'
    ? comment.text.replace(
        /Best move was (.+)\./,
        (_, san) => 'Best move was ' + renderSan(san, undefined, style),
      )
    : comment.text;
