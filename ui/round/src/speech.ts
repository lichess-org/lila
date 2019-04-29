import RoundController from './ctrl';
import { Step } from './interfaces';
import viewStatus from 'game/view/status';

export function onSoundSet(ctrl: RoundController, set: string) {
  if (!window.Speech && set === 'speech')
    window.lichess.loadScript('compiled/lichess.round.speech.min.js').then(() => {
      const s = viewStatus(ctrl);
      if (s == 'playingRightNow') window.Speech!.step(ctrl.stepAt(ctrl.ply), false);
      else {
        window.Speech!.say(s);
        const w = ctrl.data.game.winner;
        if (w) window.Speech!.say(ctrl.trans.noarg(w + 'IsVictorious'))
      }
    });
  if (window.Speech && set !== 'music') window.Speech = undefined;
}


export function userJump(ctrl: RoundController, ply: Ply) {
  if (window.Speech) window.Speech.step(ctrl.stepAt(ply));
}

export function step(step: Step) {
  if (window.Speech) window.Speech.step(step, false);
}
