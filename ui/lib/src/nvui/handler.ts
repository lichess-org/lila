import { files } from '@lichess-org/chessground/types';
import { parseFen } from 'chessops/fen';
import { chessgroundDests, lichessRules } from 'chessops/compat';
import { setupPosition } from 'chessops/variant';
import { charToRole, opposite, parseUci } from 'chessops/util';
import { destsToUcis, sanToUci, sanWriter } from '../game/chess';
import { renderPieceStr, keyFromAttrs, isKey } from './render';
import type { PieceStyle, PrefixStyle } from './setting';

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
  if (!dests || input.length < 1) return;
  const legalUcis = destsToUcis(dests),
    legalSans = sanWriter(fen, legalUcis),
    cleanedMixedCase = input[0] + input.slice(1).replace(/\+|#/g, '').toLowerCase();
  // initialize uci preserving first char of input because we need to differentiate bxc3 and Bxc3
  let uci = (sanToUci(cleanedMixedCase, legalSans) || cleanedMixedCase).toLowerCase(),
    promotion = '';

  const cleaned = cleanedMixedCase.toLowerCase();
  const drop = cleaned.match(dropRegex);
  if (drop)
    return {
      role: charToRole(cleaned[0]) || 'pawn',
      key: cleaned.split('@')[1].slice(0, 2) as Key,
    };
  if (cleaned.match(promotionRegex)) {
    uci = sanToUci(cleaned.slice(0, -2), legalSans) || cleaned;
    promotion = cleaned.slice(-1);
  } else if (cleaned.match(uciPromotionRegex)) {
    uci = cleaned.slice(0, -1);
    promotion = cleaned.slice(-1);
  } else if ('18'.includes(uci[3]) && chessground.state.pieces.get(uci.slice(0, 2) as Key)?.role === 'pawn')
    promotion = 'q';

  return legalUcis.includes(uci) ? `${uci}${promotion}` : undefined;
}

const squareSelector = (rank: string, file: string) =>
  `.board-wrapper button[rank="${rank}"][file="${file}"]`;

interface RoundStep {
  uci?: Uci;
  fen: FEN;
}
