import { propWithEffect, toggle as commonToggle } from 'common';
import * as prop from 'common/storage';
import { VoiceCtrl, VoiceModule } from './interfaces';

export * from './interfaces';
export * from './move/interfaces';
export { load as makeVoiceMove } from './move/moveCtrl';
export { renderVoiceBar } from './view';

export const VOSK_TS_VERSION = '_____1'; // this versions the wasm asset (see vosk.ts)
export const supportedLangs = [['en', 'English']];

export function makeCtrl(opts: {
  redraw: () => void;
  module?: () => VoiceModule;
  tpe: 'move' | 'coords';
}): VoiceCtrl {
  function toggle() {
    if (pushTalk()) {
      enabled(false);
      pushTalk(false);
    } else enabled(!enabled()) ? lichess.mic.start() : lichess.mic.stop();
    if (opts.tpe === 'move' && lichess.once('voice.rtfm')) showHelp(true);
  }

  function micId(deviceId?: string) {
    if (deviceId) lichess.mic.setMic(deviceId);
    return lichess.mic.micId;
  }

  const enabled = prop.storedBooleanProp('voice.on', false);

  const pushTalk = prop.storedBooleanPropWithEffect('voice.pushTalk', false, val => {
    lichess.mic.stop();
    enabled(val);
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
  const showHelp = propWithEffect<boolean | 'list'>(false, opts.redraw);

  let keyupTimeout: number;
  document.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    lichess.mic.start();
    clearTimeout(keyupTimeout);
  });
  document.addEventListener('keyup', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    clearTimeout(keyupTimeout);
    keyupTimeout = setTimeout(() => lichess.mic.stop(), 600);
  });

  lichess.mic.setLang(lang());
  if (pushTalk()) lichess.mic.start(false);
  else if (enabled()) lichess.mic.start(true);

  return {
    lang,
    micId,
    enabled,
    toggle,
    showHelp,
    pushTalk,
    showPrefs: commonToggle(false, opts.redraw),
    module: () => opts.module?.(),
    moduleId: opts.tpe,
  };
}
