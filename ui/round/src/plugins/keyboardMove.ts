import sanWriter from './sanWriter';
import { DecodedDests } from '../interfaces';

const keyRegex = /^\d{1,2}$/;

type Sans = {
  [key: string]: Uci;
}

window.lidraughts.keyboardMove = function(opts: any) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let sans: any = null;
  const submit = function(v: string, force?: boolean) {
    const foundUci = v.length >= 3 && sans && sanToUci(v, sans);
    if (foundUci) {
      opts.san(foundUci.slice(0, 2), foundUci.slice(2));
      clear();
    } else if (sans && v.match(keyRegex)) {
      if (force) {
        opts.select(v.length === 1 ? ('0' + v) : v);
        clear();
      } else
        opts.input.classList.remove('wrong');
    } else
      opts.input.classList.toggle('wrong', v.length && sans && !sanCandidates(v, sans).length);
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

function makeBindings(opts: any, submit: Function, clear: Function) {
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
    if (v.indexOf('/') > -1) {
      focusChat();
      clear();
    }
    else if (v === '' && e.which === 13) opts.confirmMove();
    else submit(v, e.which === 13);
  });
  opts.input.addEventListener('focus', function() {
    opts.setFocus(true);
  });
  opts.input.addEventListener('blur', function() {
    opts.setFocus(false);
  });
}

function sanToUci(san: string, sans: Sans): string | undefined {
  if (san in sans) return sans[san];
  if (san.length === 4 && Object.keys(sans).find(key => sans[key] === san)) return san;
  let lowered = san.toLowerCase().replace('x0', 'x').replace('-0', '-');
  if (lowered.slice(0, 1) === '0') lowered = lowered.slice(1)
  if (lowered in sans) return sans[lowered];
  return undefined
}

function sanCandidates(san: string, sans: Sans) {
  const lowered = san.toLowerCase();
  let cleanLowered = lowered.replace('x0', 'x').replace('-0', '-');
  if (cleanLowered.slice(0, 1) === '0') cleanLowered = cleanLowered.slice(1)
  var filterKeys = Object.keys(sans).filter(function(s) {
    const sLowered = s.toLowerCase();
    return sLowered.indexOf(lowered) === 0 || sLowered.indexOf(cleanLowered) === 0;
  });
  return filterKeys.length ? filterKeys : Object.keys(sans).map(key => sans[key]).filter(function(s) {
    return s.indexOf(lowered) === 0;
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
  const chatInput = document.querySelector('.mchat input.lidraughts_say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
}
