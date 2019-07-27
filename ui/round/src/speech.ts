import RoundController from './ctrl';
import viewStatus from 'game/view/status';

export function setup(ctrl: RoundController) {
  window.lidraughts.pubsub.on('speech.enabled', onSpeechChange(ctrl));
  onSpeechChange(ctrl)(window.lidraughts.sound.speech());
}

function onSpeechChange(ctrl: RoundController) {
  return function(enabled: boolean) {
    if (!window.LidraughtsSpeech && enabled)
      window.lidraughts.loadScript(
        window.lidraughts.compiledScript('speech')
      ).then(() => status(ctrl));
    else if (window.LidraughtsSpeech && !enabled) window.LidraughtsSpeech = undefined;
  };
}

export function status(ctrl: RoundController) {
  const s = viewStatus(ctrl);
  if (s == 'playingRightNow') window.LidraughtsSpeech!.step(ctrl.stepAt(ctrl.ply), false);
  else {
    withSpeech(speech => speech.say(s, false));
    const w = ctrl.data.game.winner;
    if (w) withSpeech(speech => speech.say(ctrl.noarg(w + 'IsVictorious'), false));
  }
}


export function userJump(ctrl: RoundController, ply: Ply) {
  withSpeech(s => s.step(ctrl.stepAt(ply), true));
}

export function step(step: { san?: San }) {
  withSpeech(s => s.step(step, false));
}

function withSpeech(f: (speech: LidraughtsSpeech) => void) {
  if (window.LidraughtsSpeech) f(window.LidraughtsSpeech);
}
