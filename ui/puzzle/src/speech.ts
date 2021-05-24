export function setup(): void {
  lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(lichess.sound.speech());
}

function onSpeechChange(enabled: boolean): void {
  if (!window.LichessSpeech && enabled) lichess.loadModule('speech');
  else if (window.LichessSpeech && !enabled) window.LichessSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean): void {
  withSpeech(s => s.step(n, cut));
}

export function success(): void {
  lichess.sound.say('Success!');
}

function withSpeech(f: (speech: LichessSpeech) => void): void {
  if (window.LichessSpeech) f(window.LichessSpeech);
}
