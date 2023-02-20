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
export const partialMoveRegex = /^[BNRQK]$/;

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
  return Object.keys(legalSans).filter(s => s.toLowerCase().startsWith(lowered));
}

export function destsToUcis(dests: Dests): Uci[] {
  const ucis: string[] = [];
  for (const [orig, destList] of dests) {
    for (const dest of destList) ucis.push(orig + dest);
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

const commandFunctions: Record<string, (ctrl?: MoveCtrl) => void> = {
  who: () => readOpponentName(),
  clock: (ctrl: MoveCtrl) => readClocks(ctrl.clock()),
  draw: (ctrl: MoveCtrl) => ctrl.draw(),
  resign: (ctrl: MoveCtrl) => ctrl.resign(true, true),
  next: (ctrl: MoveCtrl) => ctrl.next?.(),
  upv: (ctrl: MoveCtrl) => ctrl.vote?.(true),
  downv: (ctrl: MoveCtrl) => ctrl.vote?.(false),
  help: (ctrl: MoveCtrl) => ctrl.helpModalOpen(true),
  '?': (ctrl: MoveCtrl) => ctrl.helpModalOpen(true),
};
const commands = Object.keys(commandFunctions);

export function nonMoveCommand(command: string, ctrl: MoveCtrl, clear?: () => void): boolean {
  if (!commands.includes(command)) {
    return command.length > 0 && commands.some(c => c.startsWith(command.toLowerCase()));
  }
  commandFunctions[command](ctrl);
  clear?.();
  return true;
}
