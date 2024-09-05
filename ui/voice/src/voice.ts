import { propWithEffect, toggle as commonToggle } from 'common';
import * as prop from 'common/storage';
import { MoveRootCtrl, MoveUpdate } from 'chess/moveRootCtrl';
import type { VoiceCtrl, VoiceModule } from './interfaces';
import type { VoiceMove } from './move/interfaces';
import { Mic } from './mic';
import { flash } from './view';

export * from './interfaces';
export * from './move/interfaces';
export { renderVoiceBar } from './view';

export const supportedLangs: [string, string][] = [['en', 'English']];

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

export function makeVoice(opts: {
  redraw: () => void;
  module?: () => VoiceModule;
  tpe: 'move' | 'coords';
}): VoiceCtrl {
  let keyupTimeout: number;
  const mic = new Mic();
  const enabled = prop.storedBooleanProp('voice.on', false);
  const showHelp = propWithEffect<boolean | 'list'>(false, opts.redraw);

  const pushTalk = prop.storedBooleanPropWithEffect('voice.pushTalk', false, val => {
    mic.stop();
    enabled(val);
    if (enabled()) mic.start(!val);
  });

  const lang = prop.storedStringPropWithEffect('voice.lang', 'en', code => {
    if (code === mic.lang) return;
    mic.setLang(code);
    opts
      .module?.()
      .initGrammar()
      .then(() => {
        if (enabled()) mic.start(!pushTalk());
      });
  });
  mic.setLang(lang());

  document.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    mic.start();
    clearTimeout(keyupTimeout);
  });
  document.addEventListener('keyup', (e: KeyboardEvent) => {
    if (e.key !== 'Shift' || !pushTalk()) return;
    clearTimeout(keyupTimeout);
    keyupTimeout = setTimeout(() => mic.stop(), 600);
  });

  if (pushTalk()) mic.start(false);
  else if (enabled()) mic.start(true);

  return {
    mic,
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

  function toggle() {
    if (pushTalk()) {
      enabled(false);
      pushTalk(false);
    } else enabled(!enabled()) ? mic.start() : mic.stop();
    if (opts.tpe === 'move' && prop.once('voice.rtfm')) showHelp(true);
  }

  function micId(deviceId?: string) {
    if (deviceId) mic.setMic(deviceId);
    return mic.micId;
  }
}

export function makeVoiceMove(ctrl: MoveRootCtrl, initial: MoveUpdate): VoiceMove {
  let move: VoiceMove; // shim
  const voice = makeVoice({ redraw: ctrl.redraw, module: () => move, tpe: 'move' });
  site.asset.loadEsm<VoiceMove>('voice.move', { init: { root: ctrl, voice, initial } }).then(x => (move = x));
  return {
    ctrl: voice,
    initGrammar: () => move?.initGrammar(),
    update: (up: MoveUpdate) => move?.update(up),
    listenForResponse: (key, action) => move?.listenForResponse(key, action),
    question: () => move?.question(),
    promotionHook: () => move?.promotionHook(),
    allPhrases: () => move?.allPhrases(),
    prefNodes: () => move?.prefNodes(),
  };
}
