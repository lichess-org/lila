export function setup() {
  window.lishogi.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lishogi.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.LishogiSpeech && enabled) window.lishogi.loadScript(window.lishogi.compiledScript('speech'));
  else if (window.LishogiSpeech && !enabled) window.LishogiSpeech = undefined;
}

export function node(n: Tree.Node) {
  withSpeech(s => s.notation(n.notation, true));
}

function withSpeech(f: (speech: LishogiSpeech) => void) {
  if (window.LishogiSpeech) f(window.LishogiSpeech);
}
