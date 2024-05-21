import { objectStorage } from 'common/objectStorage';
import { Selector, Selectable } from 'common/selector';
import { storedStringProp } from 'common/storage';
import { VoskModule } from 'voice';

type Audio = { vosk?: AudioNode; source?: AudioNode; ctx?: AudioContext };

class RecNode implements Selectable {
  listenerMap = new Map<string, Voice.Listener>();
  words: string[];
  node?: AudioNode;
  partial: boolean;

  constructor(words: string[], partial: boolean) {
    this.words = words;
    this.partial = partial;
  }

  select(audio?: Audio) {
    if (!audio?.source || !this.node) return;
    audio.source.connect(this.node);
    this.node.connect(audio.ctx!.destination);
  }

  deselect() {
    this.node?.disconnect();
  }

  close() {
    this.node = undefined;
  }

  get listeners(): Voice.Listener[] {
    return [...this.listenerMap.values()].reverse(); // LIFO for interrupt
  }
}

export const mic = new (class implements Voice.Microphone {
  language = 'en';

  audioCtx: AudioContext | undefined;
  mediaStream: MediaStream;
  micSource: AudioNode;
  vosk: VoskModule;

  deviceId = storedStringProp('voice.micDeviceId', 'default');
  deviceIds?: string[];

  recs = new Selector<RecNode, Audio>();
  recId = 'default';
  ctrl: Voice.Listener;
  download?: XMLHttpRequest;
  broadcastTimeout?: number;
  voskStatus = '';
  busy = false;
  interrupt = false;
  paused = 0;

  get lang() {
    return this.language;
  }

  setLang(lang: string) {
    if (lang === this.language) return;
    this.stop();
    this.language = lang;
  }

  async getMics() {
    return navigator.mediaDevices
      .enumerateDevices()
      .then(d => d.filter(d => d.kind == 'audioinput' && d.label));
  }

  get micId() {
    return this.deviceId();
  }

  setMic(id: string) {
    const listening = this.isListening;
    site.mic.stop();
    this.deviceId(id);
    this.recs.close();
    this.audioCtx?.close();
    this.audioCtx = undefined;
    if (listening) this.start();
  }

  setController(ctrl: Voice.Listener) {
    this.ctrl = ctrl;
    this.ctrl('', 'status'); // hello
  }

  addListener(listener: Voice.Listener, also: { recId?: string; listenerId?: string } = {}) {
    const recId = also.recId ?? 'default';
    if (!this.recs.group.has(recId)) throw `No recognizer for '${recId}'`;
    this.recs.group.get(recId)!.listenerMap.set(also.listenerId ?? recId, listener);
  }

  removeListener(listenerId: string) {
    this.recs.group.forEach(v => v.listenerMap.delete(listenerId));
  }

  initRecognizer(
    words: string[],
    also: {
      recId?: string;
      partial?: boolean;
      listener?: Voice.Listener;
      listenerId?: string;
    } = {},
  ) {
    if (words.length === 0) {
      this.recs.delete(also.recId);
      return;
    }
    const recId = also.recId ?? 'default';
    const rec = new RecNode(words.slice(), also.partial === true);
    if (this.vosk?.isLoaded(this.lang)) this.initKaldi(recId, rec);
    this.recs.set(recId, rec);
    if (also.listener) this.addListener(also.listener, { recId, listenerId: also.listenerId });
  }

  setRecognizer(recId = 'default') {
    this.recId = recId;
    if (!this.isListening) return;
    this.recs.select(recId);
    this.vosk?.select(recId);
  }

  async start(listen = true): Promise<void> {
    try {
      if (listen && this.isListening && this.recId === this.recs.key) return;
      this.busy = true;
      await this.initModel();
      if (!this.busy) throw '';
      for (const [recId, rec] of this.recs.group) this.initKaldi(recId, rec);
      this.recs.select(listen && this.recId);
      this.vosk?.select(listen && this.recId);
      this.micTrack!.enabled = listen;
      this.busy = false;
      this.broadcast(listen ? 'Listening...' : '', 'start');
    } catch (e: any) {
      if (e instanceof DOMException && e.name === 'NotAllowedError') this.stop(['No permission', 'error']);
      else this.stop([e.toString(), 'error']);
      if (e !== '') throw e;
    }
  }

  stop(reason: [string, Voice.MsgType] = ['', 'stop']) {
    if (this.micTrack) this.micTrack.enabled = false;
    this.download?.abort();
    this.download = undefined;
    this.busy = false;
    this.recs.select(false);
    this.vosk?.select(false);
    this.broadcast(...reason);
  }

  // pause/resume use a counter so calls must be balanced.
  // short duration interruptions, use start/stop otherwise
  pause() {
    if (++this.paused !== 1 || !this.micTrack?.enabled) return;
    this.micTrack.enabled = false;
  }

  resume() {
    this.paused = Math.min(this.paused - 1, 0);
    if (this.paused !== 0 || this.micTrack === undefined) return;
    this.micTrack.enabled = !!this.recs.selected;
  }

  get isListening(): boolean {
    return !!this.recs.selected && !!(this.micTrack?.enabled || this.paused) && !this.isBusy;
  }

  get isBusy(): boolean {
    return this.busy;
  }

  get status(): string {
    return this.voskStatus;
  }

  stopPropagation() {
    this.interrupt = true;
  }

  /*private*/ get micTrack(): MediaStreamTrack | undefined {
    return this.mediaStream?.getAudioTracks()[0];
  }

  /*private*/ initKaldi(recId: string, rec: RecNode) {
    if (rec.node) return;
    rec.node = this.vosk?.initRecognizer({
      recId: recId,
      audioCtx: this.audioCtx!,
      partial: rec.partial,
      words: rec.words,
      broadcast: this.broadcast.bind(this),
    });
  }

  /*private*/ async initModel(): Promise<void> {
    if (this.vosk?.isLoaded(this.lang)) {
      await this.initAudio();
      return;
    }
    this.broadcast('Loading...');

    const modelUrl = site.asset.url(models.get(this.lang)!, { version: false });
    const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);
    const audioAsync = this.initAudio();

    this.vosk ??= await site.asset.loadEsm<VoskModule>('voice.vosk');

    await downloadAsync;
    await this.vosk.initModel(modelUrl, this.lang);
    await audioAsync;
  }

  /*private*/ async initAudio(): Promise<void> {
    if (this.audioCtx?.state === 'suspended') await this.audioCtx.resume();
    if (this.audioCtx?.state === 'running') return;
    else if (this.audioCtx) throw `Error ${this.audioCtx.state}`;
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      video: false,
      audio: {
        sampleRate: { ideal: 16000 },
        echoCancellation: { ideal: true },
        noiseSuppression: { ideal: true },
        deviceId: this.micId,
      },
    });
    this.audioCtx = new AudioContext({
      sampleRate: this.mediaStream.getAudioTracks()[0].getSettings().sampleRate,
    });
    this.micSource = this.audioCtx.createMediaStreamSource(this.mediaStream);
    this.recs.ctx = { vosk: this.vosk, source: this.micSource, ctx: this.audioCtx };
  }

  /*private*/ broadcast(text: string, msgType: Voice.MsgType = 'status', forMs = 0) {
    this.ctrl?.call(this, text, msgType);
    if (msgType === 'status' || msgType === 'full') window.clearTimeout(this.broadcastTimeout);
    this.voskStatus = text;
    for (const li of this.recs.get(this.recId)?.listeners ?? []) {
      if (!this.interrupt) li(text, msgType);
    }
    this.interrupt = false;
    this.broadcastTimeout = forMs > 0 ? window.setTimeout(() => this.broadcast(''), forMs) : undefined;
  }

  /*private*/ async downloadModel(emscriptenPath: string): Promise<void> {
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
      this.download.open('GET', site.asset.url(models.get(this.lang)!, { version: false }), true);
      this.download.responseType = 'arraybuffer';
      this.download.onerror = _ => reject('Failed. See console');
      this.download.onabort = _ => reject('Aborted');
      this.download.onprogress = (e: ProgressEvent) => {
        this.broadcast(
          e.total <= 0
            ? 'Downloading...'
            : `Downloaded ${Math.round((100 * e.loaded) / e.total)}% of ${Math.round(e.total / 1000000)}MB`,
        );
      };

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
