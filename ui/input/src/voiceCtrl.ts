import { VoiceCtrl, VoiceListener } from './interfaces';
import { objectStorage } from 'common/objectStorage';

const speechMap = new Map<string, string>([
  ['a', 'a'],
  ['b', 'b'],
  ['c', 'c'],
  ['d', 'd'],
  ['e', 'e'],
  ['f', 'f'],
  ['g', 'g'],
  ['h', 'h'],

  // NATO phonetic alphabet
  ['alfa', 'a'],
  ['bravo', 'b'],
  ['charlie', 'c'],
  ['delta', 'd'],
  ['echo', 'e'],
  ['foxtrot', 'f'],
  ['golf', 'g'],
  ['hotel', 'h'],

  ['one', '1'],
  ['two', '2'],
  ['three', '3'],
  ['four', '4'],
  ['five', '5'],
  ['six', '6'],
  ['seven', '7'],
  ['eight', '8'],

  ['pawn', ''],
  ['rook', 'R'],
  ['knight', 'N'],
  ['bishop', 'B'],
  ['queen', 'Q'],
  ['king', 'K'],

  ['takes', 'x'],
  ['captures', 'x'],
  ['check', ''],

  ['castle', 'o-o'],
  ['short castle', 'o-o'],
  ['long castle', 'o-o-o'],

  // Command words
  ['next', 'next'],
  ['continue', 'next'],
  ['next puzzle', 'next'],
  ['clock', 'clock'],
  ['draw', 'draw'],
  ['offer draw', 'draw'],
  ['accept draw', 'draw'],
  ['resign', 'resign'],
  ['upvote', 'upv'],
  ['downvote', 'downv'],
]);

export const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

export const makeVoiceCtrl = () =>
  new (class implements VoiceCtrl {
    audioCtx: AudioContext;
    mediaStream: MediaStream | undefined;

    voskStatus = '';
    busy = false;
    listeners = new Set<VoiceListener>();

    addListener = (listener: VoiceListener) => this.listeners.add(listener);
    removeListener = (listener: VoiceListener) => this.listeners.delete(listener);

    get isBusy(): boolean {
      return this.busy;
    }
    get status(): string {
      return this.voskStatus;
    }
    set status(cmd: string) {
      this.listen(cmd);
    }
    get isRecording(): boolean {
      return this.mediaStream !== undefined;
    }
    listen(text: string, isCommand = false) {
      this.voskStatus = text;
      const message = !isCommand
        ? text
        : text
            .split(' ')
            .flatMap(word => speechMap.get(word))
            .filter(word => word !== undefined)
            .join('');
      this.listeners.forEach(listener => listener(message, isCommand));
    }
    stop() {
      this.audioCtx?.close();
      this.mediaStream?.getAudioTracks().forEach(track => track.stop());
      this.mediaStream = undefined;
      this.listen('');
    }
    async start(): Promise<void> {
      if (this.isRecording) return Promise.resolve();
      try {
        this.busy = true;
        const firstTime = window.LichessVoice === undefined;
        if (firstTime) this.listen('Loading...');

        const modelUrl = lichess.assetUrl(modelSource, { noVersion: true });

        const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);

        if (firstTime) await lichess.loadModule('input.vosk'); // download simultaneous with model

        this.audioCtx = new AudioContext({ sampleRate: 48000 });

        await downloadAsync;

        // now we have both the model and input.vosk
        const voskNode = firstTime
          ? await window.LichessVoice.init({
              speechMap,
              impl: 'vanilla',
              audioCtx: this.audioCtx,
              listen: this.listen.bind(this),
              url: modelUrl,
            })
          : await window.LichessVoice.resume(this.audioCtx);

        this.mediaStream = await navigator.mediaDevices.getUserMedia({
          video: false,
          audio: {
            sampleRate: 48000, // vosk small acoustic models are 16kHz but webaudio doesn't like it
            channelCount: 1,
            echoCancellation: true,
            noiseSuppression: true,
          },
        });

        const micNode = this.audioCtx.createMediaStreamSource(this.mediaStream);
        micNode.connect(voskNode!);
        voskNode.connect(this.audioCtx.destination);
        this.voskStatus = 'Listening...';
      } catch (e) {
        console.log(e);
        this.stop();
        this.voskStatus = JSON.stringify(e).slice(0, 24) + '...';
        throw e;
      } finally {
        this.busy = false;
        this.listen(this.voskStatus);
      }
    }

    async downloadModel(emscriptenPath: string): Promise<void> {
      // don't look at this, it's gross.
      // trick vosk-browser into using our model by sneaking it into the emscripten IDBFS.
      // this is necessary for progress.
      const voskStore = await objectStorage<any>({
        db: '/vosk',
        store: 'FILE_DATA',
        version: 21,
        upgrade: (_, idbStore?: IDBObjectStore) => {
          // make emscripten fs happy
          idbStore?.createIndex('timestamp', 'timestamp', { unique: false });
        },
      });
      if ((await voskStore.count(`${emscriptenPath}/extracted.ok`)) > 0) return Promise.resolve();

      const modelBlob: ArrayBuffer | undefined = await new Promise((resolve, reject) => {
        const req = new XMLHttpRequest();
        req.open('GET', lichess.assetUrl(modelSource), true);
        req.responseType = 'arraybuffer';
        req.onerror = e => reject(e);
        req.onprogress = (e: ProgressEvent) =>
          this.listen(`Downloaded ${Math.round((100 * e.loaded) / e.total)}% of ${Math.round(e.total / 1000000)}MB`);

        req.send();
        req.onload = _ => {
          this.listen('Extracting...');
          resolve(req.response);
        };
      });
      const now = new Date();
      await voskStore.put(emscriptenPath, { timestamp: now, mode: 16877 });
      await voskStore.put(`${emscriptenPath}/downloaded.ok`, {
        contents: new Uint8Array([]),
        timestamp: now,
        mode: 33206,
      });
      await voskStore.put(`${emscriptenPath}/downloaded.tar.gz`, {
        contents: new Uint8Array(modelBlob!),
        timestamp: now,
        mode: 33188,
      });
      const txn = voskStore.txn('readwrite');
      txn.objectStore('FILE_DATA').index('timestamp');
    }
  })();
