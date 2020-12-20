export function setup() {
  window.lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lichess.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.LichessSpeech && enabled)
    window.lichess.loadScript(window.lichess.jsModule('speech'));
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
}

export function node(n: Tree.Node) {
  withSpeech(s => s.step(n, true));
}

function withSpeech(f: (speech: LichessSpeech) => void) {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
