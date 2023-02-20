export function voiceMove() {}
import { MoveCtrl, MoveHandler, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';
import * as util from './handlerUtil';

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  let legalSans: SanToUci | undefined;
  function submit(v: string) {
    const selectedKey = ctrl.hasSelected() || '';
    const uci = util.sanToUci(v, legalSans);

    if (v.match(util.partialMoveRegex)) {
      ctrl.voice.partialMove(v);
      return;
    }

    if (legalSans && v.match(util.fullUciRegex)) {
      ctrl.san(v.slice(0, 2) as Key, v.slice(2) as Key);
    } else if (legalSans && v.match(util.keyRegex)) {
      if (uci) ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
      else ctrl.select(v as Key);
    } else if (legalSans && uci) {
      ctrl.san(uci.slice(0, 2) as Key, uci.slice(2) as Key);
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(util.promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = util.sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
    } else if (v.match(util.crazyhouseRegex)) {
      // Incomplete crazyhouse strings such as Q@ or Q@a should do nothing.
      if (v.length > 3 || (v.length > 2 && v.startsWith('@'))) {
        if (v.length === 3) v = 'P' + v;
        ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
      }
    } else if (
      !util.nonMoveCommand(v, ctrl) &&
      v.length &&
      legalSans &&
      !util.sanCandidates(v, legalSans).length &&
      !v.match(util.fileRegex)
    ) {
      setTimeout(() => lichess.sound.play('error'), 500);
    }
  }

  ctrl.voice.addListener('moveHandler', (msgText: string, msgType: MsgType) => {
    if (msgType === 'command' && !!msgText) {
      if (ctrl.voice.partialMove()) {
        // include partial move
        submit(ctrl.voice.partialMove() + msgText);
        // clear partial move
        ctrl.voice.partialMove('');
      } else submit(msgText);
    }
    ctrl.root.redraw();
  });

  return (fen: string, dests: Dests | undefined, _: boolean) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, util.destsToUcis(dests)) : undefined;
  };
}
