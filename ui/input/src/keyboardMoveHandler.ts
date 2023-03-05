import { Submit, SubmitOpts, InputOpts, MoveHandler } from './interfaces';
import { Api as CgApi } from 'chessground/api';
import { sanWriter, SanToUci } from 'chess';
import * as util from './handlerUtil';

export function makeKeyboardHandler(opts: InputOpts): MoveHandler | undefined {
  if (opts.input?.classList.contains('ready')) return;
  opts.input?.classList.add('ready');
  let legalSans: SanToUci | null = null;

  const isKey = (v: string): v is Key => !!v.match(util.keyRegex);

  function submit(v: string, submitOpts: SubmitOpts) {
    if (!submitOpts.isTrusted) return;
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    if (v.match(util.iccfRegex)) {
      v = util.iccfToUci(v);
    }
    const foundUci = v.length >= 2 && legalSans && util.sanToUci(v, legalSans);
    const selectedKey = opts.ctrl.hasSelected() || '';

    if (legalSans && foundUci) {
      // ambiguous castle
      if (v.toLowerCase() === 'o-o' && legalSans['O-O-O'] && !submitOpts.force) return;
      // ambiguous promotion
      if (isKey(v) && (selectedKey + v).match(util.ambiguousPromotionRegex) && !submitOpts.force) return;
      // ambiguous UCI
      if (isKey(v) && selectedKey) opts.ctrl.select(v);
      // ambiguous capture+promotion (also check legalSans[v] here because bc8 could mean Bc8)
      if (v.match(util.ambiguousPromotionCaptureRegex) && legalSans[v] && !submitOpts.force) return;
      else opts.ctrl.san(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key);
      clear();
    } else if (
      legalSans &&
      selectedKey &&
      (selectedKey + v).match(util.ambiguousPromotionCaptureRegex) &&
      legalSans[selectedKey.slice(0, 1) + v.slice(0, 2)] &&
      !submitOpts.force
    ) {
      // ambiguous capture+promotion when a promotable pawn is selected; do nothing
    } else if (legalSans && isKey(v)) {
      opts.ctrl.select(v);
      clear();
    } else if (legalSans && v.match(util.fileRegex)) {
      // do nothing
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(util.promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = util.sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      opts.ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(util.crazyhouseRegex)) {
      // Incomplete crazyhouse strings such as Q@ or Q@a should do nothing.
      if (v.length > 3 || (v.length > 2 && v.startsWith('@'))) {
        if (v.length === 3) v = 'P' + v;
        opts.ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
        clear();
      }
    } else if (util.nonMoveCommand(v, opts.ctrl, clear)) {
      // gah
    } else if (submitOpts.yourMove && v.length > 0 && legalSans && !util.sanCandidates(v, legalSans).length) {
      // submitOpts.yourMove is true only when it is newly the player's turn, not on subsequent
      // updates when it is still the player's turn
      setTimeout(() => lichess.sound.play('error'), 500);
      if (opts.input) opts.input.value = '';
    } else {
      const wrong = v.length && legalSans && !util.sanCandidates(v, legalSans).length;
      if (wrong && !opts.input?.classList.contains('wrong')) lichess.sound.play('error');
      opts.input?.classList.toggle('wrong', !!wrong);
    }
  }

  function clear() {
    if (opts.input) opts.input.value = '';
    opts.input?.classList.remove('wrong');
  }

  bindKeys(opts, submit, clear);

  return (fen: string, cg: CgApi, yourMove: boolean) => {
    const dests = cg.state.movable?.dests;
    legalSans = dests && dests.size > 0 ? sanWriter(fen, util.destsToUcis(dests)) : null;
    if (opts.ctrl.voice.isRecording || opts.ctrl.voice.isBusy) return;

    submit(opts.input?.value || '', {
      isTrusted: true,
      yourMove: yourMove,
    });
  };
}

function bindKeys(opts: InputOpts, submit: Submit, clear: () => void) {
  if (!opts.input || !opts.ctrl.root.keyboard) return;
  window.Mousetrap.bind('enter', () => opts.input?.focus());
  /* keypress doesn't cut it here;
   * at the time it fires, the last typed char
   * is not available yet. Reported by:
   * https://lichess.org/forum/lichess-feedback/keyboard-input-changed-today-maybe-a-bug
   */
  opts.input.addEventListener('keyup', (e: KeyboardEvent) => {
    if (!e.isTrusted) return;
    const v = (e.target as HTMLInputElement).value;
    if (v.includes('/')) {
      const chatInput = document.querySelector('.mchat .mchat__say') as HTMLInputElement;
      if (chatInput) chatInput.focus();
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
