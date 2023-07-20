import pubsub from './pubsub';
import { assetUrl } from './assets';
import { storage } from './storage';
import { isIOS } from 'common/mobile';
import { charRole } from 'chess';

type Name = string;
type Path = string;

export type SoundMove = (node?: { san?: string; uci?: string }) => void;

export default new (class implements SoundI {
  ctx = new AudioContext();
  sounds = new Map<Path, Sound>(); // All loaded sounds and their instances
  paths = new Map<Name, Path>(); // sound names to paths
  theme = $('body').data('sound-set');
  speechStorage = storage.boolean('speech.enabled');
  volumeStorage = storage.make('sound-volume');
  baseUrl = assetUrl('sound', { version: '_____1' });
  soundMove?: SoundMove;

  async context() {
    if (this.ctx.state !== 'running' && this.ctx.state !== 'suspended') {
      // in addition to 'closed', iOS has 'interrupted'. who knows what else is out there
      this.ctx = new AudioContext();
      for (const s of this.sounds.values()) s.rewire(this.ctx);
    }
    if (this.ctx.state === 'suspended') await this.ctx.resume();

    return this.ctx;
  }

  async load(name: Name, path?: Path): Promise<Sound | undefined> {
    if (path) this.paths.set(name, path);
    else if (this.paths.has(name)) path = this.paths.get(name);
    else path ??= this.resolve(name);
    if (!path) return;
    if (this.sounds.has(path)) return this.sounds.get(path);

    const result = await fetch(`${path}.mp3`);
    if (!result.ok) throw new Error(`${path}.mp3 failed ${result.status}`);

    const arrayBuffer = await result.arrayBuffer();
    const audioBuffer = await this.ctx.decodeAudioData(arrayBuffer);
    const sound = new Sound(this.ctx, audioBuffer);
    this.sounds.set(path, sound);
    return sound;
  }

  resolve(name: Name): string | undefined {
    if (!this.enabled()) return;
    let dir = this.theme;
    if (this.theme === 'music' || this.theme === 'speech') {
      if (['move', 'capture', 'check'].includes(name)) return;
      if (name === 'genericNotify' || this.theme === 'speech') dir = 'standard';
      else dir = 'instrument';
    }
    return `${this.baseUrl}/${dir}/${name[0].toUpperCase() + name.slice(1)}`;
  }

  async play(name: Name, volume = 1): Promise<void> {
    return new Promise(resolve => {
      if (!this.enabled()) return resolve();
      this.load(name)
        .then(async sound => {
          if (!sound) return resolve();
          const resumeTimer = setTimeout(() => {
            $('#warn-no-autoplay').addClass('shown');
            resolve();
          }, 400);
          await this.context();
          clearTimeout(resumeTimer);
          sound.play(this.getVolume() * volume, resolve);
        })
        .catch(resolve);
    });
  }

  async move(node?: { san?: string; uci?: string }) {
    if (this.theme !== 'music') return;
    this.soundMove ??= await lichess.loadEsm<SoundMove>('soundMove');
    this.soundMove(node);
  }

  async countdown(count: number, interval = 500): Promise<void> {
    if (!this.enabled()) return;
    try {
      while (count > 0) {
        const promises = [new Promise(r => setTimeout(r, interval)), this.play(`countDown${count}`)];

        if (--count > 0) promises.push(this.load(`countDown${count}`));
        await Promise.all(promises);
      }
      await this.play('genericNotify');
    } catch (e) {
      console.error(e);
    }
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

  enabled = () => this.theme !== 'silent';

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

  publish = () => pubsub.emit('sound_set', this.theme);

  changeSet = (s: string) => {
    this.theme = s;
    this.publish();
    this.move();
  };

  set = () => this.theme;

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

  preloadBoardSounds() {
    for (const name of ['move', 'capture', 'check', 'genericNotify']) this.load(name);
  }
})();

class Sound {
  node: GainNode;
  ctx: AudioContext;

  constructor(ctx: AudioContext, readonly buffer: AudioBuffer) {
    this.rewire(ctx);
  }

  play(volume = 1, onend?: () => void) {
    this.node.gain.setValueAtTime(volume, this.ctx!.currentTime);
    const source = this.ctx!.createBufferSource();
    source.buffer = this.buffer;
    source.connect(this.node);
    source.onended = () => {
      source.disconnect();
      onend?.();
    };
    source.start(0);
  }

  rewire(ctx: AudioContext) {
    this.node?.disconnect();
    this.ctx = ctx;
    this.node = this.ctx.createGain();
    this.node.connect(this.ctx.destination);
  }
}
