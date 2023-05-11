import { objectStorage } from 'common/objectStorage';
import * as xhr from 'common/xhr';

class Recognizer {
  listenerMap = new Map<string, Voice.Listener>();
  vocab: string[] = [];
  node?: AudioNode;

  get listeners(): Voice.Listener[] {
    return [...this.listenerMap.values()].reverse(); // LIFO because reasons
  }
}

// this code is bundled with callers, so lazy create and store in window global
export const mic =
  window.LichessVoicePlugin?.mic ??
  new (class implements Voice.Microphone {
    language = 'en';

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

    get lang() {
      return this.language;
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

    setLang(lang: string) {
      if (lang === this.language) return;
      this.stop();
      this.language = lang;
    }

    setVocabulary(vocab: string[], mode: Voice.ListenMode = 'full') {
      const rec = this.recs[mode];
      if (vocab.length === rec.vocab.length && vocab.every((w, i) => w === rec.vocab[i])) return;
      rec.vocab = vocab;
      rec.node?.disconnect();
      rec.node = undefined;
      if (this.vosk?.isReady(this.lang)) this.initKaldi(mode);
    }

    useGrammar(name: string): Promise<any> {
      return xhr.jsonSimple(lichess.assetUrl(`compiled/grammar/${name}.json`));
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
        if (!this.recs.full.node) this.initKaldi('full');
        if (!this.recs.partial.node) this.initKaldi('partial');
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
        this.broadcast(text, msgType, 4000);
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

    initKaldi(mode: Voice.ListenMode) {
      this.recs[mode].node = this.vosk?.setRecognizer({
        mode: mode,
        audioCtx: this.audioCtx!,
        vocab: this.recs[mode].vocab,
        broadcast: this.broadcast.bind(this),
      });
    }

    async initModel(): Promise<void> {
      if (this.vosk?.isReady(this.lang)) return;
      this.broadcast('Loading...');

      const modelUrl = lichess.assetUrl(models.get(this.lang)!, { noVersion: true });
      const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);

      const audioAsync = this.initAudio();
      await lichess.loadModule('voice.vosk');
      await downloadAsync;
      await this.vosk.initModel(modelUrl, this.lang);
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
      this.recs[this.listenMode]?.node?.disconnect();
      this.micSource!.disconnect();
      this.micSource!.connect(this.recs[mode].node!);
      this.recs[mode].node!.connect(this.audioCtx!.destination);
      this.vosk.setMode(mode);
      this.listenMode = mode;
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

    broadcast(text: string, msgType: Voice.MsgType = 'status', forMs = 0) {
      if (msgType === 'partial') {
        for (const li of this.recs['partial'].listeners) li(text, msgType);
        return;
      }
      window.clearTimeout(this.broadcastTimeout);
      this.voskStatus = text;
      for (const li of this.recs['full'].listeners) {
        if (!this.interrupt) li(text, msgType);
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
        this.download.open('GET', lichess.assetUrl(models.get(this.lang)!), true);
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
          if (this.download?.status !== 200) reject(`${this.download?.status} Failed`);
          else resolve(this.download?.response);
        };
        this.download.send();
      });
      this.broadcast('Extracting...');
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

const models = new Map([
  ['ca', 'lifat/vosk/model-ca-0.4.tar.gz'],
  ['cn', 'lifat/vosk/model-cn-0.22.tar.gz'],
  ['cs', 'lifat/vosk/model-cs-0.4.tar.gz'],
  ['de', 'lifat/vosk/model-de-0.15.tar.gz'],
  ['en', 'lifat/vosk/model-en-us-0.15.tar.gz'],
  ['eo', 'lifat/vosk/model-eo-0.42.tar.gz'],
  ['es', 'lifat/vosk/model-es-0.42.tar.gz'],
  ['fa', 'lifat/vosk/model-fa-0.4.tar.gz'],
  ['fr', 'lifat/vosk/model-fr-0.22.tar.gz'],
  ['hi', 'lifat/vosk/model-hi-0.22.tar.gz'],
  ['it', 'lifat/vosk/model-it-0.22.tar.gz'],
  ['ja', 'lifat/vosk/model-ja-0.22.tar.gz'],
  ['ko', 'lifat/vosk/model-ko-0.22.tar.gz'],
  ['kz', 'lifat/vosk/model-kz-0.15.tar.gz'],
  ['nl', 'lifat/vosk/model-nl-0.22.tar.gz'],
  ['pl', 'lifat/vosk/model-pl-0.22.tar.gz'],
  ['pt', 'lifat/vosk/model-pt-0.3.tar.gz'],
  ['ru', 'lifat/vosk/model-ru-0.22.tar.gz'],
  ['tr', 'lifat/vosk/model-tr-0.3.tar.gz'],
  ['uk', 'lifat/vosk/model-uk-v3.tar.gz'],
  ['uz', 'lifat/vosk/model-uz-0.22.tar.gz'],
  ['vi', 'lifat/vosk/model-vi-0.4.tar.gz'],
]);
