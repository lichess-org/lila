import * as cg from 'chessground/types';
import { promote } from 'chess/promotion';
import { propWithEffect } from 'common';
import { voiceCtrl } from './main';
import { RootCtrl, MoveHandler, MoveCtrl, InputOpts, Submit, SubmitOpts } from './interfaces';
import { Dests, files } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';
import { keyboardBindings } from './keyboardMove';

export function makeMoveCtrl(root: RootCtrl, step: { fen: string }): MoveCtrl {
  const isFocused = propWithEffect(false, root.redraw);
  const helpModalOpen = propWithEffect(false, root.redraw);
  let handler: MoveHandler | undefined;
  let preHandlerBuffer = step.fen;
  let lastSelect = performance.now();
  const cgState = root.chessground.state;
  const select = (key: cg.Key): void => {
    if (cgState.selected === key) root.chessground.cancelMove();
    else {
      root.chessground.selectSquare(key, true);
      lastSelect = performance.now();
    }
  };
  let usedSan = false;
  return {
    drop(key, piece) {
      const role = sanToRole[piece];
      const crazyData = root.data.crazyhouse;
      const color = root.data.player.color;
      // Crazyhouse not set up properly
      if (!root.crazyValid || !root.sendNewPiece) return;
      // Square occupied
      if (!role || !crazyData || cgState.pieces.has(key)) return;
      // Piece not in Pocket
      if (!crazyData.pockets[color === 'white' ? 0 : 1][role]) return;
      if (!root.crazyValid(role, key)) return;
      root.chessground.cancelMove();
      root.chessground.newPiece({ role, color }, key);
      root.sendNewPiece(role, key, false);
    },
    promote(orig, dest, piece) {
      const role = sanToRole[piece];
      const variant = root.data.game.variant.key;
      if (!role || role == 'pawn' || (role == 'king' && variant !== 'antichess')) return;
      root.chessground.cancelMove();
      promote(root.chessground, dest, role);
      root.sendMove(orig, dest, role, { premove: false });
    },
    update(step, yourMove = false) {
      if (handler) handler(step.fen, cgState.movable.dests, yourMove);
      else preHandlerBuffer = step.fen;
    },
    registerHandler(h: MoveHandler) {
      handler = h;
      if (preHandlerBuffer) handler(preHandlerBuffer, cgState.movable.dests);
    },
    san(orig, dest) {
      usedSan = true;
      root.chessground.cancelMove();
      select(orig);
      select(dest);
      // ensure chessground does not leave the destination square selected
      root.chessground.cancelMove();
    },
    select,
    hasSelected: () => cgState.selected,
    confirmMove: () => (root.submitMove ? root.submitMove(true) : null),
    usedSan,
    jump(plyDelta: number) {
      root.userJumpPlyDelta && root.userJumpPlyDelta(plyDelta);
      root.redraw();
    },
    justSelected: () => performance.now() - lastSelect < 500,
    clock: () => root.clock,
    draw: () => (root.offerDraw ? root.offerDraw(true, true) : null),
    resign: (v, immediately) => (root.resign ? root.resign(v, immediately) : null),
    next: () => root.next?.(),
    vote: (v: boolean) => root.vote?.(v),
    helpModalOpen,
    isFocused,
    voice: voiceCtrl,
    root,
  };
}

const sanToRole: { [key: string]: cg.Role } = {
  P: 'pawn',
  N: 'knight',
  B: 'bishop',
  R: 'rook',
  Q: 'queen',
  K: 'king',
};

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@([a-h]|[a-h][1-8])?$/;
const ambiguousPromotionRegex = /^[a-h][27][a-h][18]$/;
const ambiguousPromotionCaptureRegex = /^([a-h][27]?x?)?[a-h](1|8)=?$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqkNBRQK]$/;
// accept partial ICCF because submit runs on every keypress
const iccfRegex = /^[1-8][1-8]?[1-5]?$/;

export const makeMoveHandler = (opts: InputOpts): MoveHandler | undefined => {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let legalSans: SanToUci | null = null;

  const isKey = (v: string): v is Key => !!v.match(keyRegex);

  const submit: Submit = (v: string, submitOpts: SubmitOpts) => {
    if (!submitOpts.isTrusted) return;
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    if (v.match(iccfRegex)) {
      v = iccfToUci(v);
    }
    const foundUci = v.length >= 2 && legalSans && sanToUci(v, legalSans);
    const selectedKey = opts.ctrl.hasSelected() || '';

    if (v.length > 0 && 'resign'.startsWith(v.toLowerCase())) {
      if (v.toLowerCase() === 'resign') {
        opts.ctrl.resign(true, true);
        clear();
      }
    } else if (legalSans && foundUci) {
      // ambiguous castle
      if (v.toLowerCase() === 'o-o' && legalSans['O-O-O'] && !submitOpts.force) return;
      // ambiguous promotion
      if (isKey(v) && (selectedKey + v).match(ambiguousPromotionRegex) && !submitOpts.force) return;
      // ambiguous UCI
      if (isKey(v) && selectedKey) opts.ctrl.select(v);
      // ambiguous capture+promotion (also check legalSans[v] here because bc8 could mean Bc8)
      if (v.match(ambiguousPromotionCaptureRegex) && legalSans[v] && !submitOpts.force) return;
      else opts.ctrl.san(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key);
      clear();
    } else if (
      legalSans &&
      selectedKey &&
      (selectedKey + v).match(ambiguousPromotionCaptureRegex) &&
      legalSans[selectedKey.slice(0, 1) + v.slice(0, 2)] &&
      !submitOpts.force
    ) {
      // ambiguous capture+promotion when a promotable pawn is selected; do nothing
    } else if (legalSans && isKey(v)) {
      opts.ctrl.select(v);
      clear();
    } else if (legalSans && v.match(fileRegex)) {
      // do nothing
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      opts.ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(crazyhouseRegex)) {
      // Incomplete crazyhouse strings such as Q@ or Q@a should do nothing.
      if (v.length > 3 || (v.length > 2 && v.startsWith('@'))) {
        if (v.length === 3) v = 'P' + v;
        opts.ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
        clear();
      }
    } else if (v.length > 0 && 'clock'.startsWith(v.toLowerCase())) {
      if ('clock' === v.toLowerCase()) {
        readClocks(opts.ctrl.clock());
        clear();
      }
    } else if (v.length > 0 && 'who'.startsWith(v.toLowerCase())) {
      if ('who' === v.toLowerCase()) {
        readOpponentName();
        clear();
      }
    } else if (v.length > 0 && 'draw'.startsWith(v.toLowerCase())) {
      if ('draw' === v.toLowerCase()) {
        opts.ctrl.draw();
        clear();
      }
    } else if (v.length > 0 && 'next'.startsWith(v.toLowerCase())) {
      if ('next' === v.toLowerCase()) {
        opts.ctrl.next?.();
        clear();
      }
    } else if (v.length > 0 && 'upv'.startsWith(v.toLowerCase())) {
      if ('upv' === v.toLowerCase()) {
        opts.ctrl.vote?.(true);
        clear();
      }
    } else if (v.length > 0 && 'downv'.startsWith(v.toLowerCase())) {
      if ('downv' === v.toLowerCase()) {
        opts.ctrl.vote?.(false);
        clear();
      }
    } else if (v.length > 0 && ('help'.startsWith(v.toLowerCase()) || v === '?')) {
      if (['help', '?'].includes(v.toLowerCase())) {
        opts.ctrl.helpModalOpen(true);
        clear();
      }
    } else if (submitOpts.yourMove && v.length > 0 && legalSans && !sanCandidates(v, legalSans).length) {
      // submitOpts.yourMove is true only when it is newly the player's turn, not on subsequent
      // updates when it is still the player's turn
      setTimeout(() => lichess.sound.play('error'), 500);
      opts.input.value = '';
    } else {
      const wrong = v.length && legalSans && !sanCandidates(v, legalSans).length;
      if (wrong && !opts.input.classList.contains('wrong')) lichess.sound.play('error');
      opts.input.classList.toggle('wrong', !!wrong);
    }
  };
  const clear = () => {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  opts.ctrl.voice.addListener((text: string, isCommand: boolean) => {
    if (isCommand) submit(text, { force: true, isTrusted: true });
    opts.ctrl.root.redraw();
  });
  keyboardBindings(opts, submit, clear);
  return (fen: string, dests: Dests | undefined, yourMove: boolean) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, destsToUcis(dests)) : null;
    submit(opts.input.value, {
      isTrusted: true,
      yourMove: yourMove,
    });
  };
};

function iccfToUci(v: string) {
  const chars = v.split('');
  if (chars[0]) chars[0] = files[parseInt(chars[0]) - 1];
  if (chars[2]) chars[2] = 'kqrbn'[parseInt(chars[2])];

  return chars.join('');
}

function sanToUci(san: string, legalSans: SanToUci): Uci | undefined {
  if (san in legalSans) return legalSans[san];
  const lowered = san.toLowerCase();
  for (const i in legalSans) if (i.toLowerCase() === lowered) return legalSans[i];
  return;
}

function sanCandidates(san: string, legalSans: SanToUci): San[] {
  // replace '=' in promotion moves (#7326)
  const lowered = san.replace('=', '').toLowerCase();
  return Object.keys(legalSans).filter(function (s) {
    return s.toLowerCase().startsWith(lowered);
  });
}

function destsToUcis(dests: Dests): Uci[] {
  const ucis: string[] = [];
  for (const [orig, d] of dests) {
    d.forEach(function (dest) {
      ucis.push(orig + dest);
    });
  }
  return ucis;
}

function readClocks(clockCtrl: any | undefined) {
  if (!clockCtrl) return;
  const msgs = ['white', 'black'].map(color => {
    const time = clockCtrl.millisOf(color);
    const date = new Date(time);
    const msg =
      (time >= 3600000 ? simplePlural(Math.floor(time / 3600000), 'hour') : '') +
      ' ' +
      simplePlural(date.getUTCMinutes(), 'minute') +
      ' ' +
      simplePlural(date.getUTCSeconds(), 'second');
    return `${color} ${msg}`;
  });
  lichess.sound.say(msgs.join('. '));
}

function readOpponentName(): void {
  const opponentName = document.querySelector('.ruser-top') as HTMLInputElement;
  lichess.sound.say(opponentName.innerText.split('\n')[0]);
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}
