import { propWithEffect, toggle as commonToggle } from 'common';
import * as prop from 'common/storage';
import { VoiceCtrl, VoiceModule } from './interfaces';

export * from './interfaces';
export * from './move/interfaces';
export { makeVoiceMove } from './move/shim';
export { renderVoiceBar } from './view';

export type VoiceUIOpts = {
  redraw: () => void;
  module?: () => VoiceModule;
  tpe: 'move' | 'coords';
};

export const supportedLangs = [
  ['en', 'English'] /*
  ['fr', 'Français'],
  ['de', 'Deutsch'],
  ['tr', 'Türkçe'],
  ['vi', 'Tiếng Việt'],*/,
];

export function makeCtrl(opts: VoiceUIOpts): VoiceCtrl {
  let keyupTimeout: number;
  const keydownListener = (e: KeyboardEvent) => {
    if (e.key !== 'Shift') return;
    lichess.mic.start();
    clearTimeout(keyupTimeout);
  };
  const keyupListener = (e: KeyboardEvent) => {
    if (e.key !== 'Shift') return;
    clearTimeout(keyupTimeout);
    keyupTimeout = setTimeout(() => lichess.mic.stop(), 600);
  };
  const pushTalkOn = () => {
    // TODO: setTimeout to periodically generate events (to check modifier state)
    document.addEventListener('keydown', keydownListener);
    document.addEventListener('keyup', keyupListener);
  };
  const pushTalkOff = () => {
    document.removeEventListener('keydown', keydownListener);
    document.removeEventListener('keyup', keyupListener);
  };
  const enabled = prop.storedBooleanProp('voice.on', false);
  const pushTalk = prop.storedBooleanPropWithEffect('voice.pushTalk', false, val => {
    lichess.mic.stop();
    if (val) {
      pushTalkOn();
      enabled(true);
    } else pushTalkOff();
    if (enabled()) lichess.mic.start(!val);
  });

  const lang = prop.storedStringPropWithEffect('voice.lang', 'en', code => {
    if (code === lichess.mic.lang) return;
    lichess.mic.setLang(code);
    opts
      .module?.()
      .initGrammar()
      .then(() => {
        if (enabled()) lichess.mic.start(!pushTalk());
      });
  });

  const showHelp = propWithEffect(false, opts.redraw);

  const toggle = () => {
    if (pushTalk()) {
      enabled(false);
      pushTalk(false);
    } else enabled(!enabled()) ? lichess.mic.start() : lichess.mic.stop();
    if (lichess.once('voice.rtfm')) showHelp(true);
  };

  const micId = (deviceId?: string) => {
    if (deviceId) lichess.mic.setMic(deviceId);
    return lichess.mic.micId;
  };

  lichess.mic.setLang(lang());
  if (pushTalk()) {
    pushTalkOn();
    lichess.mic.start(false);
  } else if (enabled()) lichess.mic.start(true);

  return {
    lang,
    micId,
    enabled,
    toggle,
    showHelp,
    pushTalk,
    showPrefs: commonToggle(false, opts.redraw),
    module: () => opts.module?.(),
  };
}
