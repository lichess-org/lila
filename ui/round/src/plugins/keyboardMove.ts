import { Dests } from '../interfaces';
import { sanWriter, SanToUci } from 'nvui/sanWriter';
import { KeyboardMove } from '../keyboardMove';

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@[a-h][1-8]$/;
const ambiguousPromotionCaptureRegex = /^([a-h]x?)?[a-h](1|8)$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqNBRQ]$/;

interface Opts {
  input: HTMLInputElement;
  ctrl: KeyboardMove;
}
interface SubmitOpts {
  isTrusted: boolean;
  force?: boolean;
  yourMove?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;

lichess.keyboardMove = function (opts: Opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let legalSans: SanToUci | null = null;

  const isKey = (v: string): v is Key => !!v.match(keyRegex);

  const submit: Submit = function (v: string, submitOpts: SubmitOpts) {
    if (!submitOpts.isTrusted) return;
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    const foundUci = v.length >= 2 && legalSans && sanToUci(v, legalSans);
    if (v == 'resign') {
      opts.ctrl.resign(true, true);
      clear();
    } else if (legalSans && foundUci) {
      // ambiguous castle
      if (v.toLowerCase() === 'o-o' && legalSans['O-O-O'] && !submitOpts.force) return;
      // ambiguous UCI
      if (isKey(v) && opts.ctrl.hasSelected()) opts.ctrl.select(v);
      // ambiguous promotion (also check legalSans[v] here because bc8 could mean Bc8)
      if (v.match(ambiguousPromotionCaptureRegex) && legalSans[v] && !submitOpts.force) return;
      else opts.ctrl.san(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key);
      clear();
    } else if (legalSans && isKey(v)) {
      opts.ctrl.select(v);
      clear();
    } else if (legalSans && v.match(fileRegex)) {
      // do nothing
    } else if (legalSans && v.match(promotionRegex)) {
      const foundUci = sanToUci(v.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      opts.ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(crazyhouseRegex)) {
      if (v.length === 3) v = 'P' + v;
      opts.ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
      clear();
    } else if (v.length > 0 && 'clock'.startsWith(v.toLowerCase())) {
      if ('clock' === v.toLowerCase()) {
        readClocks(opts.ctrl.clock());
        clear();
      }
    } else if (submitOpts.yourMove && v.length > 1) {
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
  makeBindings(opts, submit, clear);
  return (fen: string, dests: Dests | undefined, yourMove: boolean) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, destsToUcis(dests)) : null;
    submit(opts.input.value, {
      isTrusted: true,
      yourMove: yourMove,
    });
  };
};

function makeBindings(opts: any, submit: Submit, clear: () => void) {
  window.Mousetrap.bind('enter', () => opts.input.focus());
  /* keypress doesn't cut it here;
   * at the time it fires, the last typed char
   * is not available yet. Reported by:
   * https://lichess.org/forum/lichess-feedback/keyboard-input-changed-today-maybe-a-bug
   */
  opts.input.addEventListener('keyup', (e: KeyboardEvent) => {
    if (!e.isTrusted) return;
    const v = (e.target as HTMLInputElement).value;
    if (v.includes('/')) {
      focusChat();
      clear();
    } else if (v === '' && e.which == 13) opts.ctrl.confirmMove();
    else
      submit(v, {
        force: e.which === 13,
        isTrusted: e.isTrusted,
      });
  });
  opts.input.addEventListener('focus', () => opts.ctrl.setFocus(true));
  opts.input.addEventListener('blur', () => opts.ctrl.setFocus(false));
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.which > 36 && e.which < 41) {
      if (e.which == 37) opts.ctrl.jump(-1);
      else if (e.which == 38) opts.ctrl.jump(-999);
      else if (e.which == 39) opts.ctrl.jump(1);
      else opts.ctrl.jump(999);
      e.preventDefault();
    }
  });
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

function focusChat() {
  const chatInput = document.querySelector('.mchat .mchat__say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
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
    return `${color}: ${msg}`;
  });
  lichess.sound.say(msgs.join('. '));
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}
