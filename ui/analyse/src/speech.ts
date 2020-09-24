export function setup() {
  lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(lichess.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.LichessSpeech && enabled) lichess.loadModule('speech');
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
}

export function node(n: Tree.Node) {
  withSpeech(s => s.step(n, true));
}

function withSpeech(f: (speech: LichessSpeech) => void) {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
