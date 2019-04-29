export function setup() {
  window.lidraughts.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lidraughts.sound.speech());
}

function onSpeechChange(enabled: boolean) {
  if (!window.LidraughtsSpeech && enabled)
    window.lidraughts.loadScript(window.lidraughts.compiledScript('speech'));
  else if (window.LidraughtsSpeech && !enabled) window.LidraughtsSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean) {
  withSpeech(s => s.step(n, cut));
}

export function success() {
  withSpeech(s => s.say("Success!", false));
}

function withSpeech(f: (speech: LidraughtsSpeech) => void) {
  if (window.LidraughtsSpeech) f(window.LidraughtsSpeech);
}
