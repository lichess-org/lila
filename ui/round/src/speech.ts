import RoundController from "./ctrl";
import { Step } from "./interfaces";
import viewStatus from "game/view/status";

export function setup(ctrl: RoundController) {
  window.lishogi.pubsub.on("speech.enabled", onSpeechChange(ctrl));
  onSpeechChange(ctrl)(window.lishogi.sound.speech());
}

function onSpeechChange(ctrl: RoundController) {
  return function (enabled: boolean) {
    if (!window.LishogiSpeech && enabled)
      window.lishogi
        .loadScript(window.lishogi.compiledScript("speech"))
        .then(() => status(ctrl));
    else if (window.LishogiSpeech && !enabled) window.LishogiSpeech = undefined;
  };
}

export function status(ctrl: RoundController) {
  const s = viewStatus(ctrl);
  if (s == "playingRightNow")
    window.LishogiSpeech!.step(ctrl.stepAt(ctrl.ply), false);
  else {
    withSpeech((speech) => speech.say(s, false));
    const w = ctrl.data.game.winner === "white" ? "black" : "white";
    if (w)
      withSpeech((speech) => speech.say(ctrl.noarg(w + "IsVictorious"), false));
  }
}

export function userJump(ctrl: RoundController, ply: Ply) {
  withSpeech((s) => s.step(ctrl.stepAt(ply), true));
}

export function step(step: Step) {
  withSpeech((s) => s.step(step, false));
}

function withSpeech(f: (speech: LishogiSpeech) => void) {
  if (window.LishogiSpeech) f(window.LishogiSpeech);
}
