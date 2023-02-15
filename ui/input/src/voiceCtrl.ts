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

  // FIDE Phonetic alphabet
  ['anna', 'a'],
  ['bella', 'b'],
  ['cesar', 'c'],
  ['david', 'd'],
  ['eva', 'e'],
  ['felix', 'f'],
  ['gustav', 'g'],
  ['hector', 'h'],

  ['one', '1'],
  ['two', '2'],
  ['three', '3'],
  ['four', '4'],
  ['five', '5'],
  ['six', '6'],
  ['seven', '7'],
  ['eight', '8'],

  ['pawn', 'P'],
  ['rook', 'R'],
  ['knight', 'N'],
  ['bishop', 'B'],
  ['queen', 'Q'],
  ['king', 'K'],

  ['takes', 'x'],
  ['captures', 'x'],
  ['check', ''],
  ['to', ''],
  ['promotes', ''],
  ['equals', ''],
  ['short castle', 'o-o'],
  ['castle short', 'o-o'],
  ['king side castle', 'o-o'],
  ['castle king side', 'o-o'],

  ['long castle', 'o-o-o'],
  ['castle long', 'o-o-o'],
  ['castle queen side', 'o-o-o'],
  ['queen side castle', 'o-o-o'],

  ['castle', 'o-o'],

  // Command words
  ['next', 'next'],
  ['continue', 'next'],
  ['next puzzle', 'next'],
  ['clock', 'clock'],
  ['draw', 'draw'],
  ['offer draw', 'draw'],
  ['accept draw', 'draw'],
  ['resign', 'resign'],
  ['up vote', 'upv'],
  ['down vote', 'downv'],
]);

export const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

export const makeVoiceCtrl = () =>
  new (class implements VoiceCtrl {
    audioCtx: AudioContext;
    mediaStream: MediaStream | undefined;
    download: XMLHttpRequest | undefined;
    voskStatus = '';
    busy = false;
    broadcastTimeout: number | undefined;
    listeners = new Set<VoiceListener>();

    addListener = (listener: VoiceListener) => this.listeners.add(listener);
    removeListener = (listener: VoiceListener) => this.listeners.delete(listener);

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
      return this.mediaStream !== undefined;
    }
    broadcast(text: string, isCommand = false, forMs = 0) {
      window.clearTimeout(this.broadcastTimeout);
      this.voskStatus = text;
      const message = !isCommand
        ? text
        : text
            .split(' ')
            .flatMap(word => speechMap.get(word))
            .filter(word => word !== undefined)
            .join('');
      this.broadcastTimeout = forMs > 0 ? window.setTimeout(() => this.broadcast(''), forMs) : undefined;
      this.listeners.forEach(li => li(message, isCommand));
    }
    stop() {
      this.audioCtx?.close();
      this.download?.abort();
      this.mediaStream?.getAudioTracks().forEach(track => track.stop());
      this.mediaStream = undefined;
      this.download = undefined;
      this.broadcast('');
    }
    async start(): Promise<void> {
      if (this.isRecording) return;
      try {
        this.busy = true;
        this.broadcast('Loading...');
        const modelUrl = lichess.assetUrl(modelSource, { noVersion: true });
        const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);

        if (window.LichessVoice === undefined) {
          await lichess.loadModule('input.vosk');
        }

        this.audioCtx = new AudioContext({ sampleRate: 48000 });
        await downloadAsync;

        const voskNode = await window.LichessVoice.init({
          speechMap,
          impl: 'vanilla',
          audioCtx: this.audioCtx,
          broadcast: this.broadcast.bind(this),
          url: modelUrl,
        });

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
        this.busy = false;
        this.broadcast('');
        this.broadcast('Listening...', false, 3000);
      } catch (e: any) {
        this.busy = false;
        this.stop();
        console.log(e);
        this.broadcast(e.toString());
        throw e;
      }
    }

    async downloadModel(emscriptenPath: string): Promise<void> {
      // don't look at this, it's gross.  but we need cancel & progress.
      // trick vosk-browser into using our model by sneaking it into the emscripten IDBFS
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
          this.broadcast(`Downloaded ${Math.round((100 * e.loaded) / e.total)}% of ${Math.round(e.total / 1000000)}MB`);

        this.download.send();
        this.download.onload = _ => {
          this.broadcast('Extracting...');
          resolve(this.download?.response);
          this.download = undefined;
        };
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
      const txn = voskStore.txn('readwrite');
      txn.objectStore('FILE_DATA').index('timestamp');
    }
  })();
