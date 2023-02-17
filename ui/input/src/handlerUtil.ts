import { MoveCtrl } from './interfaces';
import { Dests, files } from 'chessground/types';
import { SanToUci } from 'chess';

export const keyRegex = /^[a-h][1-8]$/;
export const fullUciRegex = /^[a-h][1-8][a-h][1-8]$/;
export const fileRegex = /^[a-h]$/;
export const crazyhouseRegex = /^\w?@([a-h]|[a-h][1-8])?$/;
export const ambiguousPromotionRegex = /^[a-h][27][a-h][18]$/;
export const ambiguousPromotionCaptureRegex = /^([a-h][27]?x?)?[a-h](1|8)=?$/;
export const promotionRegex = /^([a-h]x?)?[a-h](1|8)=?[nbrqkNBRQK]$/;
export const iccfRegex = /^[1-8][1-8]?[1-5]?$/;

export function iccfToUci(v: string) {
  const chars = v.split('');
  if (chars[0]) chars[0] = files[parseInt(chars[0]) - 1];
  if (chars[2]) chars[2] = 'kqrbn'[parseInt(chars[2])];

  return chars.join('');
}

export function sanToUci(san: string, legalSans: SanToUci | undefined): Uci | undefined {
  if (!legalSans) return;
  if (san in legalSans) return legalSans[san];
  const lowered = san.toLowerCase();
  for (const i in legalSans) if (i.toLowerCase() === lowered) return legalSans[i];
  return;
}

export function sanCandidates(san: string, legalSans: SanToUci): San[] {
  // replace '=' in promotion moves (#7326)
  const lowered = san.replace('=', '').toLowerCase();
  return Object.keys(legalSans).filter(function (s) {
    return s.toLowerCase().startsWith(lowered);
  });
}

export function destsToUcis(dests: Dests): Uci[] {
  const ucis: string[] = [];
  for (const [orig, d] of dests) {
    d.forEach(function (dest) {
      ucis.push(orig + dest);
    });
  }
  return ucis;
}

export function readClocks(clockCtrl: any | undefined) {
  if (!clockCtrl) return;
  const msgs = ['white', 'black'].map(color => {
    const time = clockCtrl.millisOf(color);
    const date = new Date(time);
    const simplePlural = (nb: number, word: string) => `${nb} ${word}${nb != 1 ? 's' : ''}`;
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

export function readOpponentName(): void {
  const opponentName = document.querySelector('.ruser-top') as HTMLInputElement;
  lichess.sound.say(opponentName.innerText.split('\n')[0]);
}

const cmds = ['clock', 'who', 'draw', 'next', 'upv', 'downv', 'resign', 'help', '?'];
export function nonMoveCommand(cmd: string, ctrl: MoveCtrl, clear?: () => void): boolean {
  if (!cmds.includes(cmd)) {
    return cmd.length > 0 && !!cmds.find(c => c.startsWith(cmd.toLowerCase()));
  }
  if (cmd === 'clock') readClocks(ctrl.clock());
  else if (cmd === 'who') readOpponentName();
  else if (cmd === 'draw') ctrl.draw();
  else if (cmd === 'next') ctrl.next?.();
  else if (cmd === 'upv') ctrl.vote?.(true);
  else if (cmd === 'downv') ctrl.vote?.(false);
  else if (cmd === 'resign') ctrl.resign(true, true);
  else if (cmd === 'help' || cmd === '?') ctrl.helpModalOpen();
  clear?.();
  return true;
}
