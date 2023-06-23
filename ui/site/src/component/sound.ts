import pubsub from './pubsub';
import { assetUrl } from './assets';
import { storage } from './storage';
import { isIOS } from 'common/mobile';
import { charRole } from 'chess';

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

export default new (class implements SoundI {
  soundSetSounds = new Map<Name, Howl>(); // The loaded sounds and their instances
  standaloneSounds = new Map<Name, Howl>(); // Sounds that are independent of the sound set
  soundSet = $('body').data('sound-set');
  speechStorage = storage.boolean('speech.enabled');
  volumeStorage = storage.make('sound-volume');
  baseUrl = assetUrl('sound', {
    version: '_____1', // 6 random letters to update
  });

  constructor() {
    if (this.soundSet == 'music') setTimeout(this.publish, 500);
  }

  loadOggOrMp3 = (name: Name, path: Path, noSoundSet = false) =>
    (noSoundSet ? this.standaloneSounds : this.soundSetSounds).set(
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
    if (this.soundSet !== 'music')
      ['move', 'capture', 'check', 'genericNotify'].forEach(s => this.loadStandard(s));
  }

  private getOrLoadSound = (name: string, set: string): Howl => {
    let s = this.soundSetSounds.get(name) ?? this.standaloneSounds.get(name);
    if (!s) {
      this.loadStandard(name, set);
      s = this.soundSetSounds.get(name)!;
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

  playOnce(name: string): void {
    // increase chances that the first tab can put a local storage lock
    const doIt = () => {
      const storage = lichess.storage.make('just-played');
      if (Date.now() - parseInt(storage.get()!, 10) < 2000) return;
      storage.set('' + Date.now());
      this.play(name);
    };
    if (document.hasFocus()) doIt();
    else setTimeout(doIt, 10 + Math.random() * 500);
  }

  setVolume = this.volumeStorage.set;

  getVolume = () => {
    // garbage has been stored here by accident (e972d5612d)
    const v = parseFloat(this.volumeStorage.get() || '');
    return v >= 0 ? v : 0.7;
  };

  enabled = () => this.soundSet !== 'silent';

  speech = (v?: boolean): boolean => {
    if (v !== undefined) this.speechStorage.set(v);
    return this.speechStorage.get();
  };

  say = (text: string, cut = false, force = false, translated = false) => {
    try {
      if (cut) speechSynthesis.cancel();
      if (!this.speechStorage.get() && !force) return false;
      const msg = new SpeechSynthesisUtterance(text);
      msg.volume = this.getVolume();
      msg.lang = translated ? document.documentElement!.lang : 'en-US';
      if (!isIOS()) {
        // speech events are unreliable on iOS, but iphones do their own cancellation
        msg.onstart = _ => lichess.mic.pause();
        msg.onend = msg.onerror = _ => lichess.mic.resume();
      }
      speechSynthesis.speak(msg);
      return true;
    } catch (err) {
      console.error(err);
      return false;
    }
  };

  sayOrPlay = (name: string, text: string) => this.say(text) || this.play(name);

  publish = () => pubsub.emit('sound_set', this.soundSet);

  changeSet = (s: string) => {
    this.soundSet = s;
    this.soundSetSounds.clear();
    this.publish();
  };

  set = () => this.soundSet;

  saySan(san?: San, cut?: boolean) {
    const text = !san
      ? 'Game start'
      : san.includes('O-O-O#')
      ? 'long castle checkmate'
      : san.includes('O-O-O+')
      ? 'long castle check'
      : san.includes('O-O-O')
      ? 'long castle'
      : san.includes('O-O#')
      ? 'short castle checkmate'
      : san.includes('O-O+')
      ? 'short castle check'
      : san.includes('O-O')
      ? 'short castle'
      : san
          .split('')
          .map(c => {
            if (c == 'x') return 'takes';
            if (c == '+') return 'check';
            if (c == '#') return 'checkmate';
            if (c == '=') return 'promotes to';
            if (c == '@') return 'at';
            const code = c.charCodeAt(0);
            if (code > 48 && code < 58) return c; // 1-8
            if (code > 96 && code < 105) return c.toUpperCase();
            return charRole(c) || c;
          })
          .join(' ')
          .replace(/^A /, 'A, ') // "A takes" & "A 3" are mispronounced
          .replace(/(\d) E (\d)/, '$1,E $2') // Strings such as 1E5 are treated as scientific notation
          .replace(/C /, 'c ') // Capital C is pronounced as "degrees celsius" when it comes after a number (e.g. R8c3)
          .replace(/F /, 'f ') // Capital F is pronounced as "degrees fahrenheit" when it comes after a number (e.g. R8f3)
          .replace(/(\d) H (\d)/, '$1H$2'); // "H" is pronounced as "hour" when it comes after a number with a space (e.g. Rook 5 H 3)
    this.say(text, cut);
  }
})();
