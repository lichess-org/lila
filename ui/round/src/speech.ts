import { loadCompiledScript } from 'common/assets';
import viewStatus from 'game/view/status';
import { i18nFormatCapitalized } from 'i18n';
import { colorName } from 'shogi/color-name';
import { isHandicap } from 'shogiops/handicaps';
import type RoundController from './ctrl';

export function setup(ctrl: RoundController): void {
  window.lishogi.pubsub.on('speech.enabled', onSpeechChange(ctrl));
  onSpeechChange(ctrl)(window.lishogi.sound.speech());
}

function onSpeechChange(ctrl: RoundController) {
  return (enabled: boolean) => {
    if (!window.lishogi.modules.speech && enabled)
      loadCompiledScript('speech').then(() => status(ctrl));
    else if (window.lishogi.modules.speech && !enabled) window.lishogi.modules.speech = undefined;
  };
}

export function status(ctrl: RoundController): void {
  const handicap = isHandicap({
    rules: ctrl.data.game.variant.key,
    sfen: ctrl.data.game.initialSfen,
  });
  const s = viewStatus(ctrl.data.game.status, ctrl.data.game.winner, handicap);
  if (s === 'playingRightNow') notation(ctrl.stepAt(ctrl.ply)?.notation);
  else if (window.lishogi.modules.speech) {
    window.lishogi.sound.say({ en: s, jp: s }, false);
    const w = ctrl.data.game.winner;
    const text = w && i18nFormatCapitalized('xIsVictorious', colorName(w, handicap));
    if (text) window.lishogi.sound.say({ en: text, jp: text }, false);
  }
}

export function userJump(ctrl: RoundController, ply: Ply): void {
  if (window.lishogi.modules.speech)
    window.lishogi.modules.speech({ notation: ctrl.stepAt(ply)?.notation, cut: true });
}

export function notation(notation: string | undefined): void {
  if (window.lishogi.modules.speech) window.lishogi.modules.speech({ notation, cut: false });
}
