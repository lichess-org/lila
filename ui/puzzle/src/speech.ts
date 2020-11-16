export function setup(): void {
  window.lishogi.pubsub.on("speech.enabled", onSpeechChange);
  onSpeechChange(window.lishogi.sound.speech());
}

function onSpeechChange(enabled: boolean): void {
  if (!window.LishogiSpeech && enabled)
    window.lishogi.loadScript(window.lishogi.compiledScript("speech"));
  else if (window.LishogiSpeech && !enabled) window.LishogiSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean): void {
  withSpeech((s) => s.step(n, cut));
}

export function success(): void {
  withSpeech((s) => s.say("Success!", false));
}

function withSpeech(f: (speech: LishogiSpeech) => void): void {
  if (window.LishogiSpeech) f(window.LishogiSpeech);
}
