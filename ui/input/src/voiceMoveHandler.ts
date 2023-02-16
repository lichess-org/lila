export function voiceMove() {}
import { MoveCtrl, MoveHandler, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';
import * as util from './util';

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  let legalSans: SanToUci | null = null;
  function submit(v: string) {
    const sanUci = v.length >= 2 && legalSans && util.sanToUci(v, legalSans);
    const selectedKey = ctrl.hasSelected() || '';
    if (v.toLowerCase() === 'resign') {
      ctrl.resign(true, true);
      clear();
    } else if (legalSans && v.match(util.fullUciRegex)) {
      ctrl.san(v.slice(0, 2) as Key, v.slice(2) as Key);
      clear();
    } else if (legalSans && v.match(util.keyRegex)) {
      console.log(`selecting ${v}`);
      ctrl.select(v as Key);
      clear();
    } else if (legalSans && sanUci) {
      ctrl.san(sanUci.slice(0, 2) as Key, sanUci.slice(2) as Key);
    } else if (legalSans && v.match(util.fileRegex)) {
      // do nothing
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(util.promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = util.sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(util.crazyhouseRegex)) {
      // Incomplete crazyhouse strings such as Q@ or Q@a should do nothing.
      if (v.length > 3 || (v.length > 2 && v.startsWith('@'))) {
        if (v.length === 3) v = 'P' + v;
        ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
        clear();
      }
    } else if (!util.nonMoveCommand(v, ctrl) && v.length && legalSans && !util.sanCandidates(v, legalSans).length) {
      setTimeout(() => lichess.sound.play('error'), 500);
    }
  }

  function clear() {
    // we don't do partials yet
  }

  ctrl.voice.addListener('moveHandler', (msgText: string, msgType: MsgType) => {
    console.log(`got ${msgType} of ${msgText}`);
    if (msgType === 'command') submit(msgText);
    ctrl.root.redraw();
  });

  return (fen: string, dests: Dests | undefined, _: boolean) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, util.destsToUcis(dests)) : null;
  };
}
