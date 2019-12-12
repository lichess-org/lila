export function setup() {
  window.lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lichess.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.LichessSpeech && enabled)
    window.lichess.loadScript(window.lichess.compiledScript('speech'));
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean) {
  withSpeech(s => s.step(n, cut));
}

export function success() {
  withSpeech(s => s.say("Success!", false));
}

function withSpeech(f: (speech: LichessSpeech) => void) {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
