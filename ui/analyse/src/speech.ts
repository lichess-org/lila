export function setup() {
  lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(lichess.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.NewChessSpeech && enabled) lichess.loadModule('speech');
  else if (window.NewChessSpeech && !enabled) window.NewChessSpeech = undefined;
}

export function node(n: Tree.Node) {
  withSpeech(s => s.step(n, true));
}

function withSpeech(f: (speech: NewChessSpeech) => void) {
  if (window.NewChessSpeech) f(window.NewChessSpeech);
}
