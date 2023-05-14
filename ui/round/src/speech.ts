import viewStatus from 'game/view/status';
import { transWithColorName } from 'common/colorName';
import RoundController from './ctrl';
import { isHandicap } from 'shogiops/handicaps';

export function setup(ctrl: RoundController) {
  window.lishogi.pubsub.on('speech.enabled', onSpeechChange(ctrl));
  onSpeechChange(ctrl)(window.lishogi.sound.speech());
}

function onSpeechChange(ctrl: RoundController) {
  return function (enabled: boolean) {
    if (!window.LishogiSpeech && enabled)
      window.lishogi.loadScript(window.lishogi.compiledScript('speech')).then(() => status(ctrl));
    else if (window.LishogiSpeech && !enabled) window.LishogiSpeech = undefined;
  };
}

export function status(ctrl: RoundController) {
  const handicap = isHandicap({ rules: ctrl.data.game.variant.key, sfen: ctrl.data.game.initialSfen }),
    s = viewStatus(ctrl.trans, ctrl.data.game.status, ctrl.data.game.winner, handicap);
  if (s === 'playingRightNow') notation(ctrl.stepAt(ctrl.ply)?.notation);
  else {
    withSpeech(_ => window.lishogi.sound.say({ en: s, jp: s }, false));
    const w = ctrl.data.game.winner,
      text = w && transWithColorName(ctrl.trans, 'xIsVictorious', w, handicap);
    if (text) withSpeech(_ => window.lishogi.sound.say({ en: text, jp: text }, false));
  }
}

export function userJump(ctrl: RoundController, ply: Ply) {
  withSpeech(s => s.notation(ctrl.stepAt(ply)?.notation, true));
}

export function notation(notation: string | undefined) {
  withSpeech(s => s.notation(notation, false));
}

function withSpeech(f: (speech: LishogiSpeech) => void) {
  if (window.LishogiSpeech) f(window.LishogiSpeech);
}
