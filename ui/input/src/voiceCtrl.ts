import { VoiceCtrl, VoiceListener, MsgType, WordResult } from './interfaces';
import { objectStorage } from 'common/objectStorage';
import { prop } from 'common';
import { pieceCharToRole } from './util';

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

  // FIDE phonetic alphabet
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
  ['help', '?'],
  ['cancel', 'cancel'],
  ['next', 'next'],
  ['continue', 'next'],
  ['next puzzle', 'next'],
  ['clock', 'clock'],
  ['take back', 'takeback'],
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
    audioCtx?: AudioContext;
    mediaStream?: MediaStream;
    micSource?: AudioNode;
    voskNode?: AudioNode;
    download?: XMLHttpRequest;
    broadcastTimeout?: number;
    vocabulary?: string[];
    textInput: HTMLInputElement | undefined;
    voskStatus = '';
    busy = false;
    listeners = new Map<string, VoiceListener>();
    partialMove = prop('');

    addListener = (name: string, listener: VoiceListener) => this.listeners.set(name, listener);

    get isBusy(): boolean {
      return this.busy;
    }
    get status(): string {
      return this.voskStatus;
    }
    set status(status: string) {
      if (this.textInput) this.textInput.value = status;
      this.voskStatus = status;
    }
    get isRecording(): boolean {
      return !!this.mediaStream?.getAudioTracks()[0].enabled && !this.busy;
    }

    stop() {
      this.download?.abort();
      this.mediaStream?.getAudioTracks().forEach(track => (track.enabled = false));
      if (!this.download) this.broadcast('');
      this.download = undefined;
    }

    async start(): Promise<void> {
      if (this.isRecording) return;
      let [msgText, msgType] = ['Unknown', 'error' as MsgType];
      try {
        this.busy = true;
        await this.init();
        await this.updateVocabulary([...speechMap.keys()]);
        this.mediaStream!.getAudioTracks()[0].enabled = true;
        [msgText, msgType] = ['Listening...', 'status'];
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
    async updateVocabulary(vocab: string[]) {
      if (vocab.length === this.vocabulary?.length && vocab.every((value, index) => value === this.vocabulary![index]))
        return;
      const wasRecording = this.isRecording;
      this.vocabulary = vocab;
      if (this.voskNode) {
        this.voskNode.disconnect();
      }
      this.voskNode = await window.LichessVoice.initKaldi({
        audioCtx: this.audioCtx!,
        keys: vocab,
        broadcast: this.broadcast.bind(this),
        impl: 'vanilla',
      });
      this.micSource!.connect(this.voskNode!);
      this.voskNode!.connect(this.audioCtx!.destination);
      this.mediaStream!.getAudioTracks()[0].enabled = wasRecording;
    }

    async init(): Promise<void> {
      if (this.micSource) return;
      this.broadcast('Loading...');
      const modelUrl = lichess.assetUrl(modelSource, { noVersion: true });
      const downloadAsync = this.downloadModel(`/vosk/${modelUrl.replace(/[\W]/g, '_')}`);
      if (!window.LichessVoice) await lichess.loadModule('input.vosk');

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
      await window.LichessVoice.initModel(modelUrl);
    }

    broadcast(text: string, msgType: MsgType = 'status', _: WordResult | undefined = undefined, forMs = 0) {
      window.clearTimeout(this.broadcastTimeout);
      this.status = this.partialMove() ? `${pieceCharToRole[this.partialMove()]}... ${text}` : text;
      const encoded = msgType === 'command' ? this.encode(text) : text;
      for (const li of this.listeners.values()) li(encoded, msgType);
      this.broadcastTimeout = forMs > 0 ? window.setTimeout(() => this.broadcast(''), forMs) : undefined;
    }

    encode = (text: string) =>
      speechMap.get(text) ??
      text
        .split(' ')
        .flatMap(word => speechMap.get(word))
        .filter(word => word !== undefined)
        .join('');

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
