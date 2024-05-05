import { files } from 'chessground/types';
import type { SanToUci } from 'chess';
import type { Opts } from './keyboardMove';

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@([a-h]|[a-h][1-8])?$/;
const ambiguousPromotionRegex = /^[a-h][27][a-h][18]$/;
const ambiguousPromotionCaptureRegex = /^([a-h][27]?x?)?[a-h](1|8)=?$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqkNBRQK]$/;
// accept partial ICCF because submit runs on every keypress
const iccfRegex = /^[1-8][1-8]?[1-5]?$/;
const isKey = (v: string): v is Key => !!v.match(keyRegex);

interface SubmitOpts {
  isTrusted: boolean;
  force?: boolean;
  yourMove?: boolean;
}
export type Submit = (v: string, submitOpts: SubmitOpts) => void;

export function makeSubmit(opts: Opts, clear: () => void): Submit {
  // returns a Submit function that is called:
  // 1) when the user presses a key inside of the input, or
  // 2) when a move was just played
  return (v: string, submitOpts: SubmitOpts) => {
    if (!submitOpts.isTrusted) return;

    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    if (v.match(iccfRegex)) {
      v = iccfToUci(v);
    }
    const { legalSans } = opts.ctrl;
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
      const promotionSan =
        selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
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
    } else if (v.length > 0 && 'clock'.startsWith(v.toLowerCase()) && opts.ctrl.speakClock) {
      if ('clock' === v.toLowerCase()) {
        opts.ctrl.speakClock();
        clear();
      }
    } else if (v.length > 0 && 'zerk'.startsWith(v.toLowerCase()) && opts.ctrl.goBerserk) {
      if ('zerk' === v.toLowerCase()) {
        opts.ctrl.goBerserk();
        clear();
      }
    } else if (v.length > 0 && 'who'.startsWith(v.toLowerCase())) {
      if ('who' === v.toLowerCase()) {
        if (opts.ctrl.opponent) site.sound.say(opts.ctrl.opponent, false, true);
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
      setTimeout(() => site.sound.play('error'), 500);
      opts.input.value = '';
      opts.ctrl.checker?.clear();
    } else {
      const wrong = v.length && legalSans && !sanCandidates(v, legalSans).length;
      if (wrong && !opts.input.classList.contains('wrong')) site.sound.play('error');
      opts.input.classList.toggle('wrong', !!wrong);
    }
  };
}

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
