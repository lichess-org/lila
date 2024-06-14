import { propWithEffect, toggle as commonToggle } from 'common';
import * as prop from 'common/storage';
import { VoiceCtrl, VoiceModule } from './interfaces';
import { flash } from './view';

export * from './interfaces';
export * from './move/interfaces';
export { makeVoiceMove } from './move/voice.move';
export { renderVoiceBar } from './view';

export const supportedLangs = [['en', 'English']];
if (site.debug)
  supportedLangs.push(
    ['fr', 'Français'],
    ['de', 'Deutsch'],
    ['tr', 'Türkçe'],
    ['vi', 'Tiếng Việt'],
    ['ru', 'Русский'],
    ['it', 'Italiano'],
    ['sv', 'Svenska'],
  );

export function makeCtrl(opts: {
  redraw: () => void;
  module?: () => VoiceModule;
  tpe: 'move' | 'coords';
}): VoiceCtrl {
  function toggle() {
    if (pushTalk()) {
      enabled(false);
      pushTalk(false);
    } else enabled(!enabled()) ? site.mic.start() : site.mic.stop();
    if (opts.tpe === 'move' && site.once('voice.rtfm')) showHelp(true);
  }

  function micId(deviceId?: string) {
    if (deviceId) site.mic.setMic(deviceId);
    return site.mic.micId;
  }

  const enabled = prop.storedBooleanProp('voice.on', false);

  const pushTalk = prop.storedBooleanPropWithEffect('voice.pushTalk', false, val => {
    site.mic.stop();
    enabled(val);
    if (enabled()) site.mic.start(!val);
  });

  const lang = prop.storedStringPropWithEffect('voice.lang', 'en', code => {
    if (code === site.mic.lang) return;
    site.mic.setLang(code);
    opts
      .module?.()
      .initGrammar()
      .then(() => {
        if (enabled()) site.mic.start(!pushTalk());
      });
  });
  const showHelp = propWithEffect<boolean | 'list'>(false, opts.redraw);

  let keyupTimeout: number;
  document.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    site.mic.start();
    clearTimeout(keyupTimeout);
  });
  document.addEventListener('keyup', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    clearTimeout(keyupTimeout);
    keyupTimeout = setTimeout(() => site.mic.stop(), 600);
  });

  site.mic.setLang(lang());
  if (pushTalk()) site.mic.start(false);
  else if (enabled()) site.mic.start(true);

  return {
    lang,
    micId,
    enabled,
    toggle,
    showHelp,
    pushTalk,
    flash,
    showPrefs: commonToggle(false, opts.redraw),
    module: () => opts.module?.(),
    moduleId: opts.tpe,
  };
}
