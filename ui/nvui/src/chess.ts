import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { type Pieces, files } from 'chessground/types';
import { invRanks, allKeys } from 'chessground/util';
import { type Setting, makeSetting } from './setting';
import { parseFen } from 'chessops/fen';
import { chessgroundDests } from 'chessops/compat';
import { type SquareName, RULES, type Rules } from 'chessops/types';
import { setupPosition } from 'chessops/variant';
import { charToRole, parseUci, roleToChar } from 'chessops/util';
import { destsToUcis, plyToTurn, sanToUci, sanWriter } from 'chess';
import { storage } from 'common/storage';

export type Style = 'uci' | 'san' | 'literate' | 'nato' | 'anna';
export type PieceStyle = 'letter' | 'white uppercase letter' | 'name' | 'white uppercase name';
export type PrefixStyle = 'letter' | 'name' | 'none';
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

const symbolToFile = (char: string): string => skipToFile[char] ?? '';

export const supportedVariant = (key: string): boolean =>
  ['standard', 'chess960', 'kingOfTheHill', 'threeCheck', 'fromPosition', 'atomic', 'horde'].includes(key);

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

export function styleSetting(): Setting<Style> {
  return makeSetting<Style>({
    choices: [
      ['san', 'SAN: Nxf3'],
      ['uci', 'UCI: g1f3'],
      ['literate', 'Literate: knight takes f 3'],
      ['anna', 'Anna: knight takes felix 3'],
      ['nato', 'Nato: knight takes foxtrot 3'],
    ],
    default: 'anna', // all the rage in OTB blind chess tournaments
    storage: storage.make('nvui.moveNotation'),
  });
}

export function pieceSetting(): Setting<PieceStyle> {
  return makeSetting<PieceStyle>({
    choices: [
      ['letter', 'Letter: p, p'],
      ['white uppercase letter', 'White uppercase letter: P, p'],
      ['name', 'Name: pawn, pawn'],
      ['white uppercase name', 'White uppercase name: Pawn, pawn'],
    ],
    default: 'letter',
    storage: storage.make('nvui.pieceStyle'),
  });
}

export function prefixSetting(): Setting<PrefixStyle> {
  return makeSetting<PrefixStyle>({
    choices: [
      ['letter', 'Letter: w/b'],
      ['name', 'Name: white/black'],
      ['none', 'None'],
    ],
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
const renderPieceStyle = (piece: string, pieceStyle: PieceStyle) => {
  switch (pieceStyle) {
    case 'letter':
      return piece.toLowerCase();
    case 'white uppercase letter':
      return piece;
    case 'name':
      return charToRole(piece);
    case 'white uppercase name':
      return `${piece}${charToRole(piece)?.slice(1)}`;
  }
};
const renderPrefixStyle = (color: Color, prefixStyle: PrefixStyle) => {
  switch (prefixStyle) {
    case 'letter':
      return color.charAt(0);
    case 'name':
      return color + ' ';
    case 'none':
      return '';
  }
};

export function lastCaptured(
  movesGenerator: () => string[],
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
): string {
  const moves = movesGenerator();
  const oldFen = moves[moves.length - 2];
  const newFen = moves[moves.length - 1];
  if (!oldFen || !newFen) {
    return 'none';
  }
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

export function renderSan(san: San, uci: Uci | undefined, style: Style): string {
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

export function renderPieces(pieces: Pieces, style: Style): VNode {
  return h(
    'div',
    ['white', 'black'].map(color => {
      const lists: string[][] = [];
      ['king', 'queen', 'rook', 'bishop', 'knight', 'pawn'].forEach(role => {
        const keys = [];
        for (const [key, piece] of pieces) {
          if (piece.color === color && piece.role === role) keys.push(key);
        }
        if (keys.length) lists.push([`${role}${keys.length > 1 ? 's' : ''}`, ...keys]);
      });
      return h('div', [
        h('h3', `${color} pieces`),
        lists
          .map(
            l =>
              `${l[0]}: ${l
                .slice(1)
                .map((k: string) => renderKey(k, style))
                .join(', ')}`,
          )
          .join(', '),
      ]);
    }),
  );
}

export function renderPieceKeys(pieces: Pieces, p: string, style: Style): string {
  const name = `${p === p.toUpperCase() ? 'white' : 'black'} ${charToRole(p)}`;
  const res: Key[] = [];
  for (const [k, piece] of pieces) {
    if (piece && `${piece.color} ${piece.role}` === name) res.push(k as Key);
  }
  return `${name}: ${res.length ? res.map(k => renderKey(k, style)).join(', ') : 'none'}`;
}

export function renderPiecesOn(pieces: Pieces, rankOrFile: string, style: Style): string {
  const res: string[] = [];
  for (const k of allKeys) {
    if (k.includes(rankOrFile)) {
      const piece = pieces.get(k);
      if (piece) res.push(`${renderKey(k, style)} ${piece.color} ${piece.role}`);
    }
  }
  return res.length ? res.join(', ') : 'blank';
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
    const key = (file + rank) as Key;
    const piece = pieces.get(key);
    const pieceWrapper = boardStyle === 'table' ? 'td' : 'span';
    if (piece) {
      const role = roleToChar(piece.role);
      const pieceText = renderPieceStyle(piece.color === 'white' ? role.toUpperCase() : role, pieceStyle);
      const prefix = renderPrefixStyle(piece.color, prefixStyle);
      const text = renderPositionStyle(rank, file, prefix + pieceText);
      return h(pieceWrapper, doPieceButton(rank, file, role, piece.color, text));
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
  ranks.push(...invRanks.map(rank => doRank(pov, rank)));
  if (boardStyle === 'table') ranks.push(doFileHeaders());
  if (pov === 'black') ranks.reverse();
  return h(boardStyle === 'table' ? 'table.board-wrapper' : 'div.board-wrapper', ranks);
}

export const renderFile = (f: Files, style: Style): string =>
  style === 'nato' ? nato[f] : style === 'anna' ? anna[f] : f;

export const renderKey = (key: string, style: Style): string =>
  style === 'nato' || style === 'anna'
    ? `${renderFile(key[0] as Files, style)} ${key[1]}`
    : `${key[0]}${key[1]}`;

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
  return (ev: KeyboardEvent): boolean => {
    const $btn = $(ev.target as HTMLElement);
    const $file = $btn.attr('file') ?? '';
    const $rank = $btn.attr('rank') ?? '';
    let $newRank = '';
    let $newFile = '';
    if (ev.key.match(/^[1-8]$/)) {
      $newRank = ev.key;
      $newFile = $file;
    } else if (ev.key.match(/^[!@#$%^&*]$/)) {
      $newRank = $rank;
      $newFile = symbolToFile(ev.key);
      // if not a valid key for jumping
    } else {
      return true;
    }
    const newBtn = document.querySelector(
      '.board-wrapper button[rank="' + $newRank + '"][file="' + $newFile + '"]',
    ) as HTMLElement;
    if (newBtn) {
      newBtn.focus();
      return false;
    }
    return true;
  };
}

export function pieceJumpingHandler(wrapSound: () => void, errorSound: () => void) {
  return (ev: KeyboardEvent): boolean => {
    if (!ev.key.match(/^[kqrbnp]$/i)) return true;
    const $currBtn = $(ev.target as HTMLElement);

    // TODO: decouple from promotion attribute setting in selectionHandler
    if ($currBtn.attr('promotion') === 'true') {
      const $moveBox = $('input.move');
      const $boardLive = $('.boardstatus');
      const $promotionPiece = ev.key.toLowerCase();
      const $form = $moveBox.parent().parent();
      if (!$promotionPiece.match(/^[qnrb]$/)) {
        $boardLive.text('Invalid promotion piece. q for queen, n for knight, r for rook, b for bishop');
        return false;
      }
      $moveBox.val($moveBox.val() + $promotionPiece);
      $currBtn.removeAttr('promotion');
      const $sendForm = new Event('submit', {
        cancelable: true,
        bubbles: true,
      });
      $form.trigger($sendForm);
      return false;
    }

    const $myBtnAttrs =
      '.board-wrapper [rank="' + $currBtn.attr('rank') + '"][file="' + $currBtn.attr('file') + '"]';
    const $allPieces = $('.board-wrapper [piece="' + ev.key.toLowerCase() + '"], ' + $myBtnAttrs);
    const $myPieceIndex = $allPieces.index($myBtnAttrs);
    const $next = ev.key.toLowerCase() === ev.key;
    const $prevNextPieces = $next ? $allPieces.slice($myPieceIndex + 1) : $allPieces.slice(0, $myPieceIndex);
    const $piece = $next ? $prevNextPieces.get(0) : $prevNextPieces.get($prevNextPieces.length - 1);
    if ($piece) {
      $piece.focus();
      // if detected any matching piece; one is the piece being clicked on,
    } else if ($allPieces.length >= 2) {
      const $wrapPiece = $next ? $allPieces.get(0) : $allPieces.get($allPieces.length - 1);
      $wrapPiece?.focus();
      wrapSound();
    } else {
      errorSound();
    }
    return false;
  };
}

export function arrowKeyHandler(pov: Color, borderSound: () => void) {
  return (ev: KeyboardEvent): boolean => {
    const $currBtn = $(ev.target as HTMLElement);
    const $isWhite = pov === 'white';
    let $file = $currBtn.attr('file') ?? ' ';
    let $rank = Number($currBtn.attr('rank'));
    if (ev.key === 'ArrowUp') {
      $rank = $isWhite ? ($rank += 1) : ($rank -= 1);
    } else if (ev.key === 'ArrowDown') {
      $rank = $isWhite ? ($rank -= 1) : ($rank += 1);
    } else if (ev.key === 'ArrowLeft') {
      $file = String.fromCharCode($isWhite ? $file.charCodeAt(0) - 1 : $file.charCodeAt(0) + 1);
    } else if (ev.key === 'ArrowRight') {
      $file = String.fromCharCode($isWhite ? $file.charCodeAt(0) + 1 : $file.charCodeAt(0) - 1);
    } else {
      return true;
    }
    const $newSq = document.querySelector(
      '.board-wrapper [file="' + $file + '"][rank="' + $rank + '"]',
    ) as HTMLElement;
    if ($newSq) {
      $newSq.focus();
    } else {
      borderSound();
    }
    ev.preventDefault();
    return false;
  };
}

export function selectionHandler(getOpponentColor: () => Color, selectSound: () => void) {
  return (ev: MouseEvent): boolean => {
    const opponentColor = getOpponentColor();
    // this depends on the current document structure. This may not be advisable in case the structure wil change.
    const $evBtn = $(ev.target as HTMLElement);
    const $rank = $evBtn.attr('rank');
    const $pos = ($evBtn.attr('file') ?? '') + $rank;
    const $boardLive = $('.boardstatus');
    const $promotionRank = opponentColor === 'black' ? '8' : '1';
    const $moveBox = $(document.querySelector('input.move') as HTMLInputElement);
    if (!$moveBox) return false;

    // if no move in box yet
    if ($moveBox.val() === '') {
      // if user selects another's piece first
      if ($evBtn.attr('color') === opponentColor) return false;
      // as long as the user is selecting a piece and not a blank tile
      if ($evBtn.text().match(/^[^\-+]+/g)) {
        $moveBox.val($pos);
        selectSound();
      }
    } else {
      // if user selects their own piece second
      if ($evBtn.attr('color') === (opponentColor === 'black' ? 'white' : 'black')) return false;

      const $first = $moveBox.val();
      const $firstPiece = $('.board-wrapper [file="' + $first[0] + '"][rank="' + $first[1] + '"]');
      $moveBox.val($moveBox.val() + $pos);
      // this is coupled to pieceJumpingHandler() noticing that the attribute is set and acting differently. TODO: make cleaner
      // if pawn promotion
      if ($rank === $promotionRank && $firstPiece.attr('piece')?.toLowerCase() === 'p') {
        $evBtn.attr('promotion', 'true');
        $boardLive.text('Promote to? q for queen, n for knight, r for rook, b for bishop');
        return false;
      }
      // this section depends on the form being the grandparent of the input.move box.
      const $form = $moveBox.parent().parent();
      const $event = new Event('submit', {
        cancelable: true,
        bubbles: true,
      });
      $form.trigger($event);
    }
    return false;
  };
}

export function boardCommandsHandler() {
  return (ev: KeyboardEvent): boolean => {
    const $currBtn = $(ev.target as HTMLElement);
    const $boardLive = $('.boardstatus');
    const $position = ($currBtn.attr('file') ?? '') + ($currBtn.attr('rank') ?? '');
    if (ev.key === 'o') {
      $boardLive.text();
      $boardLive.text($position);
      return false;
    } else if (ev.key === 'l') {
      const $lastMove = $('p.lastMove').text();
      $boardLive.text();
      $boardLive.text($lastMove);
      return false;
    } else if (ev.key === 't') {
      $boardLive.text();
      $boardLive.text($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
      return false;
    }
    return true;
  };
}
export function lastCapturedCommandHandler(
  steps: () => string[],
  pieceStyle: PieceStyle,
  prefixStyle: PrefixStyle,
) {
  return (ev: KeyboardEvent): boolean => {
    const $boardLive = $('.boardstatus');
    if (ev.key === 'c') {
      $boardLive.text();
      $boardLive.text(lastCaptured(steps, pieceStyle, prefixStyle));
      return false;
    }
    return true;
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
    const ruleTranslation: { [vari: string]: number } = {
      standard: 0,
      antichess: 1,
      kingOfTheHill: 2,
      threeCheck: 3,
      atomic: 4,
      horde: 5,
      racingKings: 6,
      crazyhouse: 7,
    };
    const rules: Rules = RULES[ruleTranslation[variant]];

    let rawMoves: Dests | undefined;

    // possible inefficient to reparse fen; but seems to work when it is AND when it is not the users' turn. Also note that this FEN is incomplete as it only contains the piece information.
    // if it is your turn
    if (turnColor() === yourColor) {
      rawMoves = moveable();
    } else {
      const fromSetup = setupPosition(rules, parseFen(startingFen()).unwrap()).unwrap();
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

    const possibleMoves = rawMoves
      ?.get(pos)
      ?.map(i => {
        const p = pieces.get(i);
        // logic to prevent 'capture rook' on own piece in chess960
        return p && p.color !== yourColor ? `${i} captures ${p.role}` : i;
      })
      ?.filter(i => ev.key === 'm' || i.includes('captures'));
    if (!possibleMoves) {
      $boardLive.text('None');
      // if filters out non-capturing moves
    } else if (possibleMoves.length === 0) {
      $boardLive.text('No captures');
    } else {
      $boardLive.text(possibleMoves.join(', '));
    }
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

export function renderMainline(nodes: Tree.Node[], currentPath: Tree.Path, style: Style): VNodeChildren {
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

export const renderComments = (node: Tree.Node, style: Style): string =>
  node.comments?.map(c => augmentLichessComment(c, style)).join('. ') ?? '';

const augmentLichessComment = (comment: Tree.Comment, style: Style): string =>
  comment.by === 'lichess'
    ? comment.text.replace(
        /Best move was (.+)\./,
        (_, san) => 'Best move was ' + renderSan(san, undefined, style),
      )
    : comment.text;
