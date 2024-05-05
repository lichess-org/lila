import { Dests } from 'chessground/types';
import { sanWriter, destsToUcis } from 'chess';
import { KeyboardMoveHandler, KeyboardMove, isArrowKey } from './ctrl';
import { Submit, makeSubmit } from './keyboardSubmit';

export interface Opts {
  input: HTMLInputElement;
  ctrl: KeyboardMove;
}

export function load(opts: Opts): Promise<KeyboardMoveHandler> {
  return site.asset.loadEsm('keyboardMove', { init: opts });
}

export function initModule(opts: Opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');

  const clear = makeClear(opts);
  const submit = makeSubmit(opts, clear);
  makeBindings(opts, submit, clear);

  // returns a function that is called when any move is played
  return (fen: string, dests: Dests | undefined, yourMove: boolean) => {
    // update legal SAN moves
    opts.ctrl.legalSans = dests && dests.size > 0 ? sanWriter(fen, destsToUcis(dests)) : null;
    // play a premove if it is available in the input
    submit(opts.input.value, {
      isTrusted: true,
      yourMove: yourMove,
    });
  };
}

function makeClear(opts: Opts) {
  return () => {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
    opts.ctrl.checker?.clear();
  };
}

function makeBindings(opts: Opts, submit: Submit, clear: () => void) {
  site.mousetrap.bind('enter', () => opts.input.focus());
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
    } else if (v == '' && e.key == 'Enter') opts.ctrl.confirmMove();
    else {
      opts.ctrl.checker?.press(e);
      submit(v, {
        force: e.key == 'Enter',
        isTrusted: true,
      });
    }
  });
  opts.input.addEventListener('focus', () => opts.ctrl.isFocused(true));
  opts.input.addEventListener('blur', () => opts.ctrl.isFocused(false));
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', (e: KeyboardEvent) => {
    if (isArrowKey(e.key)) {
      opts.ctrl.arrowNavigate(e.key);
      e.preventDefault();
    }
  });
}

function focusChat() {
  const chatInput = document.querySelector('.mchat .mchat__say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
}
