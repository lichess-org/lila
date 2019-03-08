import sanWriter from './sanWriter';
import { DecodedDests } from '../interfaces';

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@[a-h][1-8]$/;
const ambiguousPromotionRegex = /^([a-h]x?)?[a-h](1|8)$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=\w$/;

window.lichess.keyboardMove = function(opts: any) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let sans: any = null;
  const submit = function(v: string, force?: boolean) {
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    const foundUci = v.length >= 2 && sans && sanToUci(v, sans);
    if (foundUci) {
      // ambiguous castle
      if (v.toLowerCase() === 'o-o' && sans['O-O-O'] && !force) return;
      // ambiguous UCI
      if (v.match(keyRegex) && opts.hasSelected()) opts.select(v);
      // ambiguous promotion (also check sans[v] here because bc8 could mean Bc8)
      if (v.match(ambiguousPromotionRegex) && sans[v] && !force) return;
      else opts.san(foundUci.slice(0, 2), foundUci.slice(2));
      clear();
    } else if (sans && v.match(keyRegex)) {
      opts.select(v);
      clear();
    } else if (sans && v.match(fileRegex)) {
      // do nothing
    } else if (sans && v.match(promotionRegex)) {
      const foundUci = sanToUci(v.slice(0, -2), sans);
      if (!foundUci) return;
      opts.promote(foundUci.slice(0, 2), foundUci.slice(2), v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(crazyhouseRegex)) {
      if (v.length === 3) v = 'P' + v;
      opts.drop(v.slice(2), v[0].toUpperCase());
      clear();
    } else
      opts.input.classList.toggle('wrong', v.length && sans && !sanCandidates(v, sans).length);
  };
  const clear = function() {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return function(fen: string, dests: DecodedDests) {
    sans = dests && Object.keys(dests).length ? sanWriter(fen, destsToUcis(dests)) : null;
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
    if (v.includes('/')) {
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

function sanToUci(san: string, sans: DecodedDests): Key[] | undefined {
  if (san in sans) return sans[san];
  const lowered = san.toLowerCase();
  for (let i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
  return;
}

function sanCandidates(san: string, sans: DecodedDests) {
  const lowered = san.toLowerCase();
  return Object.keys(sans).filter(function(s) {
    return s.toLowerCase().startsWith(lowered);
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
