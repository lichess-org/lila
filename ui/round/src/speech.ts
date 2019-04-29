import RoundController from './ctrl';
import { Step } from './interfaces';
import viewStatus from 'game/view/status';

export function onSpeechChange(ctrl: RoundController) {
  return function(enabled: boolean) {
    if (!window.Speech && enabled)
      window.lichess.loadScript('compiled/lichess.round.speech.min.js').then(() => status(ctrl));
    else if (window.Speech && !enabled) window.Speech = undefined;
  };
}

export function status(ctrl: RoundController) {
  const s = viewStatus(ctrl);
  if (s == 'playingRightNow') window.Speech!.step(ctrl.stepAt(ctrl.ply), false);
  else {
    window.Speech!.say(s);
    const w = ctrl.data.game.winner;
    if (w) window.Speech!.say(ctrl.trans.noarg(w + 'IsVictorious'), false)
  }
}


export function userJump(ctrl: RoundController, ply: Ply) {
  if (window.Speech) window.Speech.step(ctrl.stepAt(ply), true);
}

export function step(step: Step) {
  if (window.Speech) window.Speech.step(step, false);
}
