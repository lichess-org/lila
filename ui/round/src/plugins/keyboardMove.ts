import sanWriter from './sanWriter';
import { DecodedDests } from '../interfaces';

const keyRegex = /^\d{1,2}$/;

type Sans = {
  [key: string]: Uci;
}

interface SubmitOpts {
  force?: boolean;
  server?: boolean;
  yourMove?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;

window.lidraughts.keyboardMove = function(opts: any) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let sans: any = null;
  const submit: Submit = function(v: string, submitOpts: SubmitOpts) {
    const foundUci = v.length >= 3 && sans && sanToUci(v, sans);
    if (foundUci) {
      opts.ctrl.san(foundUci.slice(0, 2), foundUci.slice(2));
      clear();
    } else if (sans && v.match(keyRegex)) {
      if (submitOpts.force) {
        opts.ctrl.select(v.length === 1 ? ('0' + v) : v);
        clear();
      } else
        opts.input.classList.remove('wrong');
    } else if (v.toLowerCase().startsWith('clock')) {
      readClocks(opts.ctrl.clock());
      clear();
    } else if (submitOpts.yourMove && v.length > 1) {
      setTimeout(window.lidraughts.sound.error, 500);
      opts.input.value = '';
    }
    else {
      const wrong = v.length && sans && !sanCandidates(v, sans).length;
      if (wrong && !opts.input.classList.contains('wrong')) window.lidraughts.sound.error();
      opts.input.classList.toggle('wrong', wrong);
    }
  };
  const clear = function() {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return function(fen: string, dests: DecodedDests, captLen?: number) {
    sans = dests && Object.keys(dests).length ? sanWriter(fen, destsToUcis(dests), captLen) : null;
    submit(opts.input.value);
  };
}

function makeBindings(opts: any, submit: Submit, clear: Function) {
  window.Mousetrap.bind('enter', function() {
    opts.input.focus();
  });
  /* keypress doesn't cut it here;
   * at the time it fires, the last typed char
   * is not available yet. Reported by:
   * https://lichess.org/forum/lichess-feedback/keyboard-input-changed-today-maybe-a-bug
   */
  opts.input.addEventListener('keyup', function(e: KeyboardEvent) {
    const v = (e.target as HTMLInputElement).value;
    if (v.includes('/')) {
      focusChat();
      clear();
    }
    else if (v === '' && e.which == 13) opts.ctrl.confirmMove();
    else submit(v, {
      force: e.which === 13
    });
  });
  opts.input.addEventListener('focus', function() {
    opts.ctrl.setFocus(true);
  });
  opts.input.addEventListener('blur', function() {
    opts.ctrl.setFocus(false);
  });
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', function(e: KeyboardEvent) {
    if (e.which > 36 && e.which < 41) {
      if (e.which == 37) opts.ctrl.jump(-1);
      else if (e.which == 38) opts.ctrl.jump(-999);
      else if (e.which == 39) opts.ctrl.jump(1);
      else  opts.ctrl.jump(999);
      e.preventDefault();
    }
  });
}

function sanToUci(san: string, sans: Sans): string | undefined {
  if (san in sans) return sans[san];
  if (san.length === 4 && Object.keys(sans).find(key => sans[key] === san)) return san;
  let lowered = san.toLowerCase().replace('x0', 'x').replace('-0', '-');
  if (lowered.startsWith('0')) lowered = lowered.slice(1)
  if (lowered in sans) return sans[lowered];
  return undefined
}

function sanCandidates(san: string, sans: Sans) {
  const lowered = san.toLowerCase();
  let cleanLowered = lowered.replace('x0', 'x').replace('-0', '-');
  if (cleanLowered.startsWith('0')) cleanLowered = cleanLowered.slice(1)
  var filterKeys = Object.keys(sans).filter(function(s) {
    const sLowered = s.toLowerCase();
    return sLowered.startsWith(lowered) || sLowered.startsWith(cleanLowered);
  });
  return filterKeys.length ? filterKeys : Object.keys(sans).map(key => sans[key]).filter(function(s) {
    return s.startsWith(lowered);
  });
}

function destsToUcis(dests: DecodedDests) {
  const ucis: string[] = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
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
    const msg = (time >= 3600000 ? simplePlural(Math.floor(time / 3600000), 'hour') : '') + ' ' +
      simplePlural(date.getUTCMinutes(), 'minute') +  ' ' +
      simplePlural(date.getUTCSeconds(), 'second');
    return `${color}: ${msg}`;
  });
  window.lidraughts.sound.say(msgs.join('. '));
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}
