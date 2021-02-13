export function setup(): void {
  window.lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lichess.sound.speech());
}

function onSpeechChange(enabled: boolean): void {
  if (!window.LichessSpeech && enabled)
    window.lichess.loadScript(window.lichess.jsModule('speech'));
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean): void {
  withSpeech(s => s.step(n, cut));
}

export function success(): void {
  withSpeech(s => s.say('Success!', false));
}

function withSpeech(f: (speech: LichessSpeech) => void): void {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
