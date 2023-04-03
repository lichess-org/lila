import { objectStorage } from 'common/objectStorage';
import * as xhr from 'common/xhr';

const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

class Recognizer {
  listenerMap = new Map<string, Voice.Listener>();
  vocab: string[] = [];
  node?: AudioNode;

  get listeners(): Voice.Listener[] {
    return [...this.listenerMap.values()].reverse(); // LIFO
  }
}

// mic supports at most two simultaneous recognizers, one for full phrases and the other for partials
export const mic =
  window.LichessVoicePlugin?.mic ??
  new (class implements Voice.Microphone {
    grammar?: { name: string; callback: (g: Voice.Entry[]) => void };

    audioCtx: AudioContext;
    mediaStream: MediaStream;
    micSource: AudioNode;

    listenMode: Voice.ListenMode;
    recs: { full: Recognizer; partial: Recognizer };
    download?: XMLHttpRequest;
    broadcastTimeout?: number;
    voskStatus = '';
    busy = false;
    interrupt = false;
    paused = 0;

    constructor() {
      (window.LichessVoicePlugin as any) = { mic: this, vosk: undefined };
      lichess.mic = this;
      this.recs = { full: new Recognizer(), partial: new Recognizer() };
    }

    get vosk(): any {
      return (window.LichessVoicePlugin as any)?.vosk;
    }

    addListener(id: string, listener: Voice.Listener, mode: Voice.ListenMode = 'full') {
      this.recs[mode].listenerMap.set(id, listener);
    }

    removeListener(id: string) {
      Object.values(this.recs).forEach(rec => rec.listenerMap.delete(id));
    }

    async setVocabulary(vocab: string[], mode: Voice.ListenMode = 'full') {
      const rec = this.recs[mode];
      if (vocab.length === rec.vocab.length && vocab.every((w, i) => w === rec.vocab[i])) return;
      rec.vocab = vocab;
      if (!this.vosk?.ready()) return;
      this.stop();
      await this.initKaldi(mode);
    }

    useGrammar(name: string, callback: (g: Voice.Entry[]) => void) {
      this.grammar = { name: name, callback: callback };
    }

    stop() {
      if (this.micTrack) this.micTrack.enabled = false;
      this.vosk?.stop();
      this.download?.abort();
      if (!this.download) this.broadcast('', 'stop');
      this.download = undefined;
    }

    async start(): Promise<void> {
      let [text, msgType] = ['Unknown', 'error' as Voice.MsgType];
      try {
        if (this.isListening) return;
        this.busy = true;
        await this.initModel();
        await this.initKaldi();
        this.micTrack!.enabled = true;
        this.mode = 'full';
        [text, msgType] = ['Listening...', 'start'];
      } catch (e: any) {
        this.stop();
        console.log(e);
        [text, msgType] = [e.toString(), 'error'];
        throw e;
      } finally {
        this.busy = false;
        this.broadcast(text, msgType, undefined, 4000);
      }
    }

    // pause/resume use a counter so calls must be balanced.
    pause() {
      if (++this.paused !== 1 || !this.micTrack?.enabled) return;
      this.micTrack.enabled = false;
      this.broadcast('Paused...', 'status');
    }

    resume() {
      this.paused = Math.min(this.paused - 1, 0);
      if (this.paused !== 0 || this.micTrack?.enabled === undefined) return;
      this.micTrack.enabled = true;
      this.broadcast('Listening...', 'status');
    }

    async initKaldi(mode?: Voice.ListenMode) {
      if (!mode && Object.values(this.recs).every(r => r.node || r.vocab.length === 0)) return;
      const modes = mode ? [mode] : (['full', 'partial'] as Voice.ListenMode[]);
      for (const m of modes) {
        const rec = this.recs[m];
        if (!mode && (rec.node || rec.vocab.length === 0)) continue;
        rec.node = await this.vosk?.setRecognizer({
          mode: m,
          audioCtx: this.audioCtx!,
          vocab: rec.vocab,
          broadcast: this.broadcast.bind(this),
        });
      }
    }

    async initModel(): Promise<void> {
      if (this.vosk?.ready()) return;
      this.broadcast('Loading...');
      const modelUrl = lichess.assetUrl(modelSource, { noVersion: true });
      const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);
      const grammarAsync = this.grammar
        ? xhr.json(lichess.assetUrl(`grammar/${this.grammar.name}.json`))
        : Promise.resolve();
      const audioAsync = this.initAudio();
      await lichess.loadModule('input.vosk');
      this.grammar?.callback((await grammarAsync) as Voice.Entry[]);
      await downloadAsync;
      await this.vosk.initModel(modelUrl);
      await audioAsync;
    }

    async initAudio(): Promise<void> {
      if (this.audioCtx) return;
      this.audioCtx = new AudioContext();
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: false,
        audio: {
          sampleRate: this.audioCtx.sampleRate,
          echoCancellation: true,
          noiseSuppression: true,
        },
      });
      this.micSource = this.audioCtx.createMediaStreamSource(this.mediaStream);
    }

    set mode(mode: Voice.ListenMode) {
      if (!this.recs[mode]) return;
      const hookup = () => {
        this.recs[this.listenMode]?.node?.disconnect();
        this.micSource!.disconnect();
        this.micSource!.connect(this.recs[mode].node!);
        this.recs[mode].node!.connect(this.audioCtx!.destination);
        this.vosk.setMode(mode);
        this.listenMode = mode;
      };
      if (mode === 'partial') this.initKaldi(mode).then(hookup); // partial recs crap out after one use
      else hookup();
    }

    get mode(): Voice.ListenMode {
      return this.listenMode;
    }

    get isBusy(): boolean {
      return this.busy;
    }

    get status(): string {
      return this.voskStatus;
    }

    get isListening(): boolean {
      return this.paused === 0 && !this.busy && this.micTrack?.enabled === true;
    }

    get micTrack(): MediaStreamTrack | undefined {
      return this.mediaStream?.getAudioTracks()[0];
    }

    broadcast(
      text: string,
      msgType: Voice.MsgType = 'status',
      words: Voice.WordResult | undefined = undefined,
      forMs = 0
    ) {
      if (msgType === 'partial') {
        for (const li of this.recs['partial'].listeners) li(text, msgType);
        return;
      }
      window.clearTimeout(this.broadcastTimeout);
      this.voskStatus = text;
      for (const li of this.recs['full'].listeners) {
        if (!this.interrupt) li(text, msgType, words);
      }
      this.interrupt = false;
      this.broadcastTimeout = forMs > 0 ? window.setTimeout(() => this.broadcast(''), forMs) : undefined;
    }

    stopPropagation() {
      this.interrupt = true;
    }

    async downloadModel(emscriptenPath: string): Promise<void> {
      const voskStore = await objectStorage<any>({
        db: '/vosk',
        store: 'FILE_DATA',
        version: 21,
        upgrade: (_, idbStore?: IDBObjectStore) => {
          // make emscripten fs happy
          idbStore?.createIndex('timestamp', 'timestamp', { unique: false });
        },
      });
      if ((await voskStore.count(`${emscriptenPath}/extracted.ok`)) > 0) return;

      const modelBlob: ArrayBuffer | undefined = await new Promise((resolve, reject) => {
        this.download = new XMLHttpRequest();
        this.download.open('GET', lichess.assetUrl(modelSource), true);
        this.download.responseType = 'arraybuffer';
        this.download.onerror = _ => reject('Failed. See console');
        this.download.onabort = _ => reject('Aborted');
        this.download.onprogress = (e: ProgressEvent) =>
          this.broadcast(
            e.total <= 0
              ? 'Downloading...'
              : `Downloaded ${Math.round((100 * e.loaded) / e.total)}% of ${Math.round(e.total / 1000000)}MB`
          );

        this.download.onload = _ => {
          this.broadcast('Extracting...');
          resolve(this.download?.response);
        };
        this.download.send();
      });
      const now = new Date();
      await voskStore.put(emscriptenPath, { timestamp: now, mode: 16877 });
      await voskStore.put(`${emscriptenPath}/downloaded.ok`, {
        contents: new Uint8Array([]),
        timestamp: now,
        mode: 33206,
      });
      await voskStore.remove(`${emscriptenPath}/downloaded.tar.gz`);
      await voskStore.put(`${emscriptenPath}/downloaded.tar.gz`, {
        contents: new Uint8Array(modelBlob!),
        timestamp: now,
        mode: 33188,
      });
      voskStore.txn('readwrite').objectStore('FILE_DATA').index('timestamp');
    }
  })();
