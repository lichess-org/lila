export function voiceMove() {}
import { MoveCtrl, MoveHandler, MsgType } from './interfaces';
import { Dests } from 'chessground/types';
import { sanWriter, SanToUci } from 'chess';

export function makeVoiceHandler(ctrl: MoveCtrl): MoveHandler {
  let legalSans: SanToUci | null = null;
  function submit(v: string) {
    const foundUci = v.length >= 2 && legalSans && sanToUci(v, legalSans);
    const selectedKey = ctrl.hasSelected() || '';

    if (v.length > 0 && 'resign'.startsWith(v.toLowerCase())) {
      if (v.toLowerCase() === 'resign') {
        ctrl.resign(true, true);
        clear();
      }
    } else if (legalSans && foundUci) {
      ctrl.san(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key);
      clear();
    } else if (legalSans && v.match(keyRegex)) {
      ctrl.select(v as Key);
      clear();
    } else if (legalSans && v.match(fileRegex)) {
      // do nothing
    } else if (legalSans && (selectedKey.slice(0, 1) + v).match(promotionRegex)) {
      const promotionSan = selectedKey && selectedKey.slice(0, 1) !== v.slice(0, 1) ? selectedKey.slice(0, 1) + v : v;
      const foundUci = sanToUci(promotionSan.replace('=', '').slice(0, -1), legalSans);
      if (!foundUci) return;
      ctrl.promote(foundUci.slice(0, 2) as Key, foundUci.slice(2) as Key, v.slice(-1).toUpperCase());
      clear();
    } else if (v.match(crazyhouseRegex)) {
      // Incomplete crazyhouse strings such as Q@ or Q@a should do nothing.
      if (v.length > 3 || (v.length > 2 && v.startsWith('@'))) {
        if (v.length === 3) v = 'P' + v;
        ctrl.drop(v.slice(2) as Key, v[0].toUpperCase());
        clear();
      }
    } else if (v.length > 0 && 'clock'.startsWith(v.toLowerCase())) {
      if ('clock' === v.toLowerCase()) {
        readClocks(ctrl.clock());
        clear();
      }
    } else if (v.length > 0 && 'who'.startsWith(v.toLowerCase())) {
      if ('who' === v.toLowerCase()) {
        readOpponentName();
        clear();
      }
    } else if (v.length > 0 && 'draw'.startsWith(v.toLowerCase())) {
      if ('draw' === v.toLowerCase()) {
        ctrl.draw();
        clear();
      }
    } else if (v.length > 0 && 'next'.startsWith(v.toLowerCase())) {
      if ('next' === v.toLowerCase()) {
        ctrl.next?.();
        clear();
      }
    } else if (v.length > 0 && 'upv'.startsWith(v.toLowerCase())) {
      if ('upv' === v.toLowerCase()) {
        ctrl.vote?.(true);
        clear();
      }
    } else if (v.length > 0 && 'downv'.startsWith(v.toLowerCase())) {
      if ('downv' === v.toLowerCase()) {
        ctrl.vote?.(false);
        clear();
      }
    } else if (v.length > 0 && ('help'.startsWith(v.toLowerCase()) || v === '?')) {
      if (['help', '?'].includes(v.toLowerCase())) {
        ctrl.helpModalOpen(true);
        clear();
      }
    } else if (legalSans && !sanCandidates(v, legalSans).length) {
      // submitOpts.yourMove is true only when it is newly the player's turn, not on subsequent
      // updates when it is still the player's turn
      setTimeout(() => lichess.sound.play('error'), 500);
    } else {
      const wrong = v.length && legalSans && !sanCandidates(v, legalSans).length;
      if (wrong) {
        // maybe set status to red here
        lichess.sound.play('error');
      }
    }
  }
  function clear() {
    // we don't do partials yet
  }

  ctrl.voice.addListener((msgText: string, msgType: MsgType) => {
    if (msgType === 'command') submit(msgText);
    ctrl.root.redraw();
  });

  return (fen: string, dests: Dests | undefined) => {
    legalSans = dests && dests.size > 0 ? sanWriter(fen, destsToUcis(dests)) : null;
  };
}

const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;
const crazyhouseRegex = /^\w?@([a-h]|[a-h][1-8])?$/;
//const ambiguousPromotionRegex = /^[a-h][27][a-h][18]$/;
//const ambiguousPromotionCaptureRegex = /^([a-h][27]?x?)?[a-h](1|8)=?$/;
const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqkNBRQK]$/;

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

function readOpponentName(): void {
  const opponentName = document.querySelector('.ruser-top') as HTMLInputElement;
  lichess.sound.say(opponentName.innerText.split('\n')[0]);
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}
