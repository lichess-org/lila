export function setup(): void {
  window.lishogi.pubsub.on('speech.enabled', onSpeechChange);
  onSpeechChange(window.lishogi.sound.speech());
}

function onSpeechChange(enabled: boolean): void {
  if (!window.LishogiSpeech && enabled) window.lishogi.loadScript(window.lishogi.compiledScript('speech'));
  else if (window.LishogiSpeech && !enabled) window.LishogiSpeech = undefined;
}

export function node(n: Tree.Node, cut: boolean): void {
  withSpeech(s => s.notation(n.notation, cut));
}

export function failure(): void {
  withSpeech(_ => window.lishogi.sound.say({ en: 'Failed!', jp: '失敗！' }, false));
}

export function success(): void {
  withSpeech(_ => window.lishogi.sound.say({ en: 'Success!', jp: '成功！' }, false));
}

function withSpeech(f: (speech: LishogiSpeech) => void): void {
  if (window.LishogiSpeech) f(window.LishogiSpeech);
}
