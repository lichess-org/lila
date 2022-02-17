export function setup(): void {
  lichess.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(lichess.sound.speech());
}

function onSpeechChange(enabled: boolean): void {
  if (!window.NewChessSpeech && enabled) lichess.loadModule('speech');
  else if (window.NewChessSpeech && !enabled) window.NewChessSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean): void {
  withSpeech(s => s.step(n, cut));
}

export function success(): void {
  lichess.sound.say('Success!');
}

function withSpeech(f: (speech: NewChessSpeech) => void): void {
  if (window.NewChessSpeech) f(window.NewChessSpeech);
}
