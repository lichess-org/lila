import { Dests } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';
import { KeyboardMove } from '../main';

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@([a-h]|[a-h][1-8])?$/;
const ambiguousPromotionRegex = /^[a-h][27][a-h][18]$/;
const ambiguousPromotionCaptureRegex = /^([a-h][27]?x?)?[a-h](1|8)=?$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqkNBRQK]$/;

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

export default (opts: Opts) => {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let legalSans: SanToUci | null = null;

  const isKey = (v: string): v is Key => !!v.match(keyRegex);

  const submit: Submit = (v: string, submitOpts: SubmitOpts) => {
    if (!submitOpts.isTrusted) return;
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
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
    } else if (v.length > 0 && 'draw'.startsWith(v.toLowerCase())) {
      if ('draw' === v.toLowerCase()) {
        opts.ctrl.draw();
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
  makeBindings(opts, submit, clear);

  // JUNK speech code
  if ('webkitSpeechRecognition' in window) {
    console.log('found speech recognition API');

    interface IWindow extends Window {
      webkitSpeechRecognition: any;
    }

    const { webkitSpeechRecognition } = window as unknown as IWindow;
    const recognition = new webkitSpeechRecognition();
    recognition.continuous = true;
    recognition.lang = 'en-US';
    recognition.maxAlternatives = 5;

    const massagePart = (part: string) => {
      if (['to', 'takes'].includes(part)) return '';
      const replacements: { [token: string]: string } = {
        before: 'b4',
        define: 'd5',
        asics: 'a6',
        esox: 'e6',
      };
      if (replacements[part]) return replacements[part];
      if (part.match(/8[0-8]/)) return part.replace('8', 'a');
      if (part.match(/t[0-8]/)) return part.replace('t', 'd');
      return part
        .replace(/alpha/g, 'a')
        .replace(/bravo|bee|be/g, 'b')
        .replace(/charlie|see|sea/g, 'c')
        .replace(/delta/g, 'd')
        .replace(/echo/g, 'e')
        .replace(/foxtrot/g, 'f')
        .replace(/golf/g, 'g')
        .replace(/hotel|each/g, 'h')
        .replace(/knight|night/g, 'N')
        .replace(/bishop/g, 'B')
        .replace(/brooke|brook|rook/g, 'R')
        .replace(/queen/g, 'Q')
        .replace(/king/g, 'K')
        .replace(/promote/g, '=')
        .replace(/one|won/g, '1')
        .replace(/two|too/g, '2')
        .replace(/three/g, '3')
        .replace(/four|for/g, '4')
        .replace(/five/g, '5')
        .replace(/six/g, '6')
        .replace(/seven/g, '7')
        .replace(/eight|ate/g, '8')
        .replace(/\s/g, '');
    };
    const massage = (transcript: string): string => {
      const parts = transcript.toLowerCase().split(' ');
      return parts.map(massagePart).join('');
    };

    // recognition.onstart = function() { ... }
    const validMove = (move: string): boolean => !!move.match(/^[NBRQK]?[a-h]?[1-8]?[a-h][1-8]$/);
    recognition.onresult = (event: any) => {
      let moveToSubmit = '';
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        const transcripts = [];
        const massages = [];
        for (const j of [0, 1, 2, 3, 4]) {
          if (!event.results[i][j]) continue;
          transcripts.push(event.results[i][j].transcript);
          massages.push(massage(event.results[i][j].transcript));
          if (validMove(massages[j])) {
            console.log(massages[j]);
            if (!moveToSubmit) moveToSubmit = massages[j];
          }
        }
        console.log(transcripts.join(' || '));
        console.log(massages.join(' || '));
      }
      if (moveToSubmit) submit(moveToSubmit, { isTrusted: true, yourMove: false });
    };
    // recognition.onerror = function(event) { ... }
    recognition.onend = function () {
      recognition.start();
    };
    recognition.start();
  }
  // JUNK

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
  opts.input.addEventListener('focus', () => opts.ctrl.isFocused(true));
  opts.input.addEventListener('blur', () => opts.ctrl.isFocused(false));
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
    return `${color} ${msg}`;
  });
  lichess.sound.say(msgs.join('. '));
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}
