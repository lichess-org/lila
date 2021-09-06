import pubsub from './pubsub';
import { assetUrl } from './assets';
import { storage } from './storage';

declare class Howl {
  constructor(opts: { src: string | string[] });
  volume(vol: number): Howl;
  play(): number;
}

interface Howler {
  ctx: AudioContext;
}

declare const Howler: Howler;

type Name = string;
type Path = string;

const sound: SoundI = new (class {
  sounds = new Map<Name, Howl>(); // The loaded sounds and their instances
  soundSet = $('body').data('sound-set');
  speechStorage = storage.makeBoolean('speech.enabled');
  volumeStorage = storage.make('sound-volume');
  baseUrl = assetUrl('sound', {
    version: '_____1', // 6 random letters to update
  });

  constructor() {
    if (this.soundSet == 'music') setTimeout(this.publish, 500);
  }

  loadOggOrMp3 = (name: Name, path: Path) =>
    this.sounds.set(
      name,
      new Howl({
        src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`),
      })
    );

  loadStandard = (name: Name, soundSet?: string) => {
    if (!this.enabled()) return;
    const path = name[0].toUpperCase() + name.slice(1);
    this.loadOggOrMp3(name, `${this.baseUrl}/${soundSet || this.soundSet}/${path}`);
  };

  preloadBoardSounds() {
    if (this.soundSet !== 'music') ['move', 'capture', 'check', 'genericNotify'].forEach(s => this.loadStandard(s));
  }

  private getOrLoadSound = (name: string, set: string): Howl => {
    let s = this.sounds.get(name);
    if (!s) {
      this.loadStandard(name, set);
      s = this.sounds.get(name)!;
    }
    return s;
  };

  play(name: string, volume?: number) {
    if (!this.enabled()) return;
    let set = this.soundSet;
    if (set === 'music' || this.speechStorage.get()) {
      if (['move', 'capture', 'check'].includes(name)) return;
      set = 'standard';
    }
    const s = this.getOrLoadSound(name, set);

    const doPlay = () => s.volume(this.getVolume() * (volume || 1)).play();
    if (Howler.ctx?.state === 'suspended') Howler.ctx.resume().then(doPlay);
    else doPlay();
  }

  setVolume = this.volumeStorage.set;

  getVolume = () => {
    // garbage has been stored here by accident (e972d5612d)
    const v = parseFloat(this.volumeStorage.get() || '');
    return v >= 0 ? v : 0.7;
  };

  enabled = () => this.soundSet !== 'silent';

  speech = (v?: boolean): boolean => {
    if (typeof v != 'undefined') this.speechStorage.set(v);
    return this.speechStorage.get();
  };

  say = (text: string, cut = false, force = false, translated = false) => {
    if (cut) speechSynthesis.cancel();
    if (!this.speechStorage.get() && !force) return false;

    const msg = new SpeechSynthesisUtterance(text);
    msg.volume = this.getVolume();
    msg.lang = translated ? document.documentElement!.lang : 'en-US';
    speechSynthesis.speak(msg);
    return true;
  };

  sayOrPlay = (name: string, text: string) => this.say(text) || this.play(name);

  publish = () => pubsub.emit('sound_set', this.soundSet);

  changeSet = (s: string) => {
    this.soundSet = s;
    this.sounds.clear();
    this.publish();
  };

  set = () => this.soundSet;
})();

export default sound;
