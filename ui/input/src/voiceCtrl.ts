import { VoiceCtrl, VoiceListener } from './interfaces';

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

// export const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';
export const modelSource = 'vendor/vosk/spammy-model.tar.gz';

export const makeVoiceCtrl = () =>
  new (class implements VoiceCtrl {
    audioCtx: AudioContext;
    mediaStream: MediaStream | undefined;

    voskStatus = '';
    listeners = new Set<VoiceListener>();

    addListener = (listener: VoiceListener) => this.listeners.add(listener);
    removeListener = (listener: VoiceListener) => this.listeners.delete(listener);

    get status(): string {
      return this.voskStatus;
    }
    set status(cmd: string) {
      this.listen(cmd);
    }
    get recording(): boolean {
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
    stop = () => {
      this.audioCtx?.close();
      this.mediaStream?.getAudioTracks().forEach(track => track.stop());
      this.mediaStream = undefined;
      this.listen('');
    };
    async start(): Promise<void> {
      if (this.recording) return Promise.resolve();
      try {
        const firstTime = window.LichessVoice === undefined;
        if (firstTime) this.listen('Initializing...');

        const fakeModelUrl = `https://lichess.org/${modelSource}`;
        const sandboxPath = `/vosk/${fakeModelUrl.replace(/[\W]/g, '_')}`;

        const downloadPromise = this.downloadModelTo(sandboxPath);

        if (firstTime) await lichess.loadModule('input.vosk'); // download simultaneous with model

        this.audioCtx = new AudioContext({ sampleRate: 48000 });

        await downloadPromise;

        // now we have both the model and input.vosk
        const voskNode = firstTime
          ? await window.LichessVoice.init({
              speechMap,
              impl: 'vanilla',
              audioCtx: this.audioCtx,
              listen: this.listen.bind(this),
              url: lichess.assetUrl(modelSource), // fakeModelUrl
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
        this.listen('');
      } catch (e) {
        console.log(e);
        this.voskStatus = JSON.stringify(e).slice(0, 24) + '...';
        this.stop();
        throw e;
      }
    }

    downloadModelTo(sandboxPath: string): Promise<void> {
      // we need to trigger the model download here and put it in sandbox filesystem
      // at this path to trick the webassembly to use ours.
      const assetUrl = lichess.assetUrl(modelSource);
      assetUrl;
      sandboxPath;
      return Promise.resolve();
    }
  })();
