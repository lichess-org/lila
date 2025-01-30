import { assetUrl } from 'common/assets';
import { capitalize } from 'common/string';
import throttle from 'common/throttle';
import { pubsub } from './pubsub';
import { storage } from './storage';

type Name = string;
type Path = string;

export function createSound(): typeof window.lishogi.sound {
  window.Howler.autoSuspend = false;

  const state = {
    sounds: new Map<Name, Howl>(),
    soundSet: document.body.dataset.soundSet!,
    speechStorage: storage.makeBoolean('speech.enabled'),
    volumeStorage: storage.make('sound-volume'),
    baseUrl: assetUrl('sound', { noVersion: true }),
  };

  if (state.soundSet === 'music') {
    setTimeout(() => publish(), 500);
  }

  function loadOggOrMp3(name: Name, path: Path) {
    return state.sounds.set(
      name,
      new window.Howl({
        src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`),
      }),
    );
  }

  function loadStandard(name: Name, soundSet?: string) {
    const path = capitalize(name);
    loadOggOrMp3(name, `${state.baseUrl}/${soundSet || state.soundSet}/${path}`);
  }

  function preloadGameSounds(clock = false) {
    if (state.soundSet !== 'music') {
      ['move', 'capture', 'check', 'genericNotify'].forEach(s => loadStandard(s));
      if (clock) {
        setTimeout(() => ['tick', 'lowtime', 'period'].forEach(s => loadStandard(s)), 1000 * 5);
      }
    }
  }

  function play(name: string, volume?: number) {
    if (!enabled()) return;
    let set = state.soundSet;
    if (set === 'music' || state.speechStorage.get()) {
      if (['move', 'capture', 'check'].includes(name)) return;
      set = 'shogi';
    }
    let s = state.sounds.get(name);
    if (!s) {
      loadStandard(name, set);
      s = state.sounds.get(name);
    }
    window.Howler.volume(getVolume());

    const doPlay = () => {
      s!.volume(getVolume() * (volume || 1));
      s!.play();
    };
    if (window.Howler.ctx?.state === 'suspended') {
      window.Howler.ctx.resume().then(doPlay);
    } else {
      doPlay();
    }
  }

  function throttlePlay(name: string, delay?: number, volume?: number): () => void {
    return throttle(delay || 100, () => play(name, volume));
  }

  function setVolume(value: number) {
    return state.volumeStorage.set(value);
  }

  function getVolume() {
    const v = Number.parseFloat(state.volumeStorage.get() || '');
    return v >= 0 ? v : 0.7;
  }

  function enabled() {
    return state.soundSet !== 'silent';
  }

  function speech(v?: boolean): boolean {
    if (typeof v !== 'undefined') {
      state.speechStorage.set(v);
    }
    return state.speechStorage.get();
  }

  function say(texts: { en?: string; jp?: string }, cut = false, force = false) {
    try {
      if (!state.speechStorage.get() && !force) return false;
      const useJp = !!texts.jp && document.documentElement.lang === 'ja';
      const text = useJp ? texts.jp : texts.en;
      const lang = useJp ? 'ja-JP' : 'en-US';
      const msg = new SpeechSynthesisUtterance(text);
      msg.volume = getVolume();
      msg.lang = lang;
      if (cut) speechSynthesis.cancel();
      speechSynthesis.speak(msg);
      console.log(`%c${msg.text}`, 'color: blue');
      return true;
    } catch (err) {
      console.error(err);
      return false;
    }
  }

  function sayOrPlay(name: string, texts: { en?: string; jp?: string }): boolean {
    return say(texts) || play(name) || false;
  }

  function publish() {
    pubsub.emit('sound_set', state.soundSet);
  }

  function changeSet(s: string) {
    state.soundSet = s;
    state.sounds.clear();
    publish();
  }

  function getSet() {
    return state.soundSet;
  }

  return {
    preloadGameSounds,
    play,
    throttlePlay,
    setVolume,
    getVolume,
    enabled,
    speech,
    say,
    sayOrPlay,
    publish,
    changeSet,
    set: getSet,
    loadStandard,
  };
}
