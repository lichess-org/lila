import { objectStorage } from 'common/objectStorage';

const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

export const mic: Voice.Microphone =
  window.LichessVoicePlugin?.mic ??
  new (class implements Voice.Microphone {
    audioCtx?: AudioContext;
    mediaStream?: MediaStream;
    micSource?: AudioNode;
    voskNode?: AudioNode;
    download?: XMLHttpRequest;
    broadcastTimeout?: number;
    vocabulary: string[] = [];
    voskStatus = '';
    busy = false;
    paused = false; // for ignoring speech synthesis, less overhead than start/stop
    pauseStack = 0;
    listeners = new Map<string, Voice.Listener>();

    constructor() {
      (window.LichessVoicePlugin as any) = { mic: this, vosk: undefined };
      lichess.mic = this;
    }

    addListener(name: string, listener: Voice.Listener) {
      this.listeners.set(name, listener);
    }

    removeListener(name: string) {
      this.listeners.delete(name);
    }

    async setVocabulary(vocab: string[]) {
      if (vocab.length === this.vocabulary.length && vocab.every((value, index) => value === this.vocabulary[index]))
        return;
      this.vocabulary = vocab;
      if (window.LichessVoicePlugin.vosk) await this.initKaldi(true);
    }

    get isBusy(): boolean {
      return this.busy;
    }
    get status(): string {
      return this.voskStatus;
    }
    set status(status: string) {
      this.voskStatus = status;
    }
    get isRecording(): boolean {
      return !!this.mediaStream?.getAudioTracks()[0].enabled && !this.busy;
    }

    stop() {
      this.paused = false;
      this.download?.abort();
      this.mediaStream?.getAudioTracks().forEach(track => (track.enabled = false));
      if (!this.download) this.broadcast('', 'stop');
      this.download = undefined;
    }

    async start(): Promise<void> {
      let [msgText, msgType] = ['Unknown', 'error' as Voice.MsgType];
      try {
        if (this.isRecording) return;
        this.busy = true;
        await this.initModel();
        await this.initKaldi();
        this.mediaStream!.getAudioTracks()[0].enabled = !this.paused;
        [msgText, msgType] = ['Listening...', 'start'];
      } catch (e: any) {
        this.voskNode?.disconnect();
        this.micSource?.disconnect();
        this.audioCtx?.close();
        this.voskNode = undefined;
        this.micSource = undefined;
        this.audioCtx = undefined;
        this.stop();
        console.log(e);
        [msgText, msgType] = [e.toString(), 'error'];
        throw e;
      } finally {
        this.busy = false;
        this.broadcast(msgText, msgType, undefined, 4000);
      }
    }

    pushPause() {
      if (++this.pauseStack !== 1) return;
      this.paused = true;
      if (this.mediaStream) this.mediaStream.getAudioTracks()[0].enabled = false;
    }

    popPause() {
      if (--this.pauseStack !== 0) return;
      this.paused = false;
      if (this.mediaStream) this.mediaStream.getAudioTracks()[0].enabled = true;
    }

    async initKaldi(force = false) {
      if (!this.vocabulary || (!force && this.voskNode)) return;
      const wasRecording = this.isRecording;
      this.mediaStream!.getAudioTracks()[0].enabled = false;

      if (this.voskNode) this.voskNode.disconnect();
      this.voskNode = await window.LichessVoicePlugin.vosk.initKaldi({
        audioCtx: this.audioCtx!,
        keys: this.vocabulary,
        broadcast: this.broadcast.bind(this),
        impl: 'vanilla',
      });
      this.micSource!.connect(this.voskNode!);
      this.voskNode!.connect(this.audioCtx!.destination);
      this.mediaStream!.getAudioTracks()[0].enabled = wasRecording;
    }

    async initModel(): Promise<void> {
      if (this.micSource) return;
      this.broadcast('Loading...');
      const modelUrl = lichess.assetUrl(modelSource, { noVersion: true });
      const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);
      if (!window.LichessVoicePlugin.vosk) await lichess.loadModule('input.vosk');

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
      await downloadAsync;
      await window.LichessVoicePlugin.vosk.initModel(modelUrl);
    }

    broadcast(
      text: string,
      msgType: Voice.MsgType = 'status',
      words: Voice.WordResult | undefined = undefined,
      forMs = 0
    ) {
      window.clearTimeout(this.broadcastTimeout);
      this.status = text;
      for (const li of [...this.listeners.values()].reverse()) if (li(text, msgType, words)) break;
      this.broadcastTimeout = forMs > 0 ? window.setTimeout(() => this.broadcast(''), forMs) : undefined;
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
