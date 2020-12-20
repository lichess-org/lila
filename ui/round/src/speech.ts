import RoundController from './ctrl';
import { Step } from './interfaces';
import viewStatus from 'game/view/status';

export function setup(ctrl: RoundController) {
  window.lichess.pubsub.on('speech.enabled', onSpeechChange(ctrl));
  onSpeechChange(ctrl)(window.lichess.sound.speech());
}

function onSpeechChange(ctrl: RoundController) {
  return function(enabled: boolean) {
    if (!window.LichessSpeech && enabled)
      window.lichess.loadScript(
        window.lichess.jsModule('speech')
      ).then(() => status(ctrl));
    else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
  };
}

export function status(ctrl: RoundController) {
  const s = viewStatus(ctrl);
  if (s == 'playingRightNow') window.LichessSpeech!.step(ctrl.stepAt(ctrl.ply), false);
  else {
    withSpeech(speech => speech.say(s, false));
    const w = ctrl.data.game.winner;
    if (w) withSpeech(speech => speech.say(ctrl.noarg(w + 'IsVictorious'), false));
  }
}


export function userJump(ctrl: RoundController, ply: Ply) {
  withSpeech(s => s.step(ctrl.stepAt(ply), true));
}

export function step(step: Step) {
  withSpeech(s => s.step(step, false));
}

function withSpeech(f: (speech: LichessSpeech) => void) {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
